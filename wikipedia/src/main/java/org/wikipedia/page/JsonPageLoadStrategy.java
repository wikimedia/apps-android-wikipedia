package org.wikipedia.page;

import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.editing.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.bottomcontent.BottomContentHandler;
import org.wikipedia.page.bottomcontent.BottomContentInterface;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.wikipedia.page.PageViewFragmentInternal.SUBSTATE_NONE;
import static org.wikipedia.page.PageViewFragmentInternal.SUBSTATE_SAVED_PAGE_LOADED;

/**
 * Our old page load strategy, which uses the JSON MW API directly and loads a page in multiple steps:
 * First it loads the lead section (sections=0).
 * Then it loads the remaining sections (sections=1-).
 * <p/>
 * This class tracks:
 * - the states the page loading goes through,
 * - a backstack of pages and page positions visited,
 * - and many handlers.
 */
public class JsonPageLoadStrategy implements PageLoadStrategy {
    private static final String TAG = "JsonPageLoad";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    private int state = STATE_NO_FETCH;
    private int subState = SUBSTATE_NONE;

    /**
     * List of lightweight history items to serve as the backstack for this fragment.
     * Since the list consists of Parcelable objects, it can be saved and restored from the
     * savedInstanceState of the fragment.
     */
    private ArrayList<PageBackStackItem> backStack = new ArrayList<>();

    /**
     * Sequence number to maintain synchronization when loading page content asynchronously
     * between the Java and Javascript layers, as well as between async tasks and the UI thread.
     */
    private int pageSequenceNum;

    /**
     * The y-offset position to which the page will be scrolled once it's fully loaded
     * (or loaded to the point where it can be scrolled to the correct position).
     */
    private int stagedScrollY;
    private int sectionTargetFromIntent;
    private String sectionTargetFromTitle;

    /**
     * Whether to write the page contents to cache as soon as it's loaded.
     */
    private boolean cacheOnComplete = true;

    // copied fields
    private PageViewModel model;
    private PageViewFragmentInternal fragment;
    private CommunicationBridge bridge;
    private PageActivity activity;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikipediaApp app;
    private LeadImagesHandler leadImagesHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private EditHandler editHandler;

    private BottomContentInterface bottomContentHandler;

    @Override
    public void setup(PageViewModel model, PageViewFragmentInternal fragment,
                      SwipeRefreshLayoutWithScroll refreshView,
                      ObservableWebView webView, CommunicationBridge bridge,
                      SearchBarHideHandler searchBarHideHandler, LeadImagesHandler leadImagesHandler) {
        this.model = model;
        this.fragment = fragment;
        activity = (PageActivity) fragment.getActivity();
        this.app = (WikipediaApp) activity.getApplicationContext();
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
        this.searchBarHideHandler = searchBarHideHandler;
        this.leadImagesHandler = leadImagesHandler;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        setupSpecificMessageHandlers();

        pageSequenceNum = 0;

        if (savedInstanceState != null) {
            ArrayList<PageBackStackItem> tmpBackStack
                    = savedInstanceState.getParcelableArrayList("backStack");
            if (tmpBackStack != null) { // avoid setting backStack to null
                backStack = tmpBackStack;
            }
        }

        // if we already have pages in the backstack (whether it's from savedInstanceState, or
        // from being stored in the activity's fragment backstack), then load the topmost page
        // on the backstack.
        loadPageFromBackStack();
    }

    private void setupSpecificMessageHandlers() {
        bridge.addListener("onBeginNewPage", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != pageSequenceNum) {
                        return;
                    }
                    stagedScrollY = messagePayload.getInt("stagedScrollY");
                    loadPageOnWebViewReady(messagePayload.getBoolean("tryFromCache"));
                } catch (JSONException e) {
                    //nope
                }
            }
        });
        bridge.addListener("requestSection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != pageSequenceNum) {
                        return;
                    }
                    displayNonLeadSection(messagePayload.getInt("index"));
                } catch (JSONException e) {
                    //nope
                }
            }
        });
        bridge.addListener("pageLoadComplete", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!fragment.isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != pageSequenceNum) {
                        return;
                    }
                } catch (JSONException e) {
                    // nope
                }
                // Do any other stuff that should happen upon page load completion...
                activity.updateProgressBar(false, true, 0);

                // trigger layout of the bottom content
                // Check to see if the page title has changed (e.g. due to following a redirect),
                // because if it has then the handler needs the new title to make sure it doesn't
                // accidentally display the current article as a "read more" suggestion
                if (!bottomContentHandler.getTitle().equals(model.getTitle())) {
                    bottomContentHandler.setTitle(model.getTitle());
                }
                bottomContentHandler.beginLayout();
            }
        });

        bottomContentHandler = new BottomContentHandler(fragment, bridge, webView,
                fragment.getLinkHandler(),
                (ViewGroup) fragment.getView().findViewById(R.id.bottom_content_container));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // update the topmost entry in the backstack
        updateBackStackItem();
        outState.putParcelableArrayList("backStack", backStack);
    }

    @Override
    public void backFromEditing(Intent data) {
        //Retrieve section ID from intent, and find correct section, so where know where to scroll to
        sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
        //reset our scroll offset, since we have a section scroll target
        stagedScrollY = 0;
    }

    @Override
    public void onDisplayNewPage(boolean pushBackStack, boolean tryFromCache, int stagedScrollY) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateBackStackItem();
            pushBackStack();
        }

        state = STATE_NO_FETCH;
        subState = SUBSTATE_NONE;

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        pageSequenceNum++;

        // kick off an event to the WebView that will cause it to clear its contents,
        // and then report back to us when the clearing is complete, so that we can synchronize
        // the transitions of our native components to the new page content.
        // The callback event from the WebView will then call the loadPageOnWebViewReady()
        // function, which will continue the loading process.
        try {
            JSONObject wrapper = new JSONObject();
            // whatever we pass to this event will be passed back to us by the WebView!
            wrapper.put("sequence", pageSequenceNum);
            wrapper.put("tryFromCache", tryFromCache);
            wrapper.put("stagedScrollY", stagedScrollY);
            bridge.sendMessage("beginNewPage", wrapper);
        } catch (JSONException e) {
            //nope
        }
    }

    private void performActionForState(int forState) {
        if (!fragment.isAdded()) {
            return;
        }
        switch (forState) {
            case STATE_NO_FETCH:
                activity.updateProgressBar(true, true, 0);
                // hide the lead image...
                leadImagesHandler.hide();
                bottomContentHandler.hide();
                activity.getSearchBarHideHandler().setFadeEnabled(false);
                new LeadSectionFetchTask(pageSequenceNum).execute();
                break;
            case STATE_INITIAL_FETCH:
                new RestSectionsFetchTask(pageSequenceNum).execute();
                break;
            case STATE_COMPLETE_FETCH:
                editHandler.setPage(model.getPage());
                // kick off the lead image layout
                leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                    @Override
                    public void onLayoutComplete() {
                        if (!fragment.isAdded()) {
                            return;
                        }
                        searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                        // when the lead image layout is complete, load the lead section and
                        // the other sections into the webview.
                        displayLeadSection();
                        displayNonLeadSection(1);
                    }
                });
                break;
            default:
                // This should never happen
                throw new RuntimeException("Unknown state encountered " + state);
        }
    }

    private void setState(int state) {
        setState(state, SUBSTATE_NONE);
    }

    private void setState(int state, int subState) {
        if (!fragment.isAdded()) {
            return;
        }
        this.state = state;
        this.subState = subState;
        activity.supportInvalidateOptionsMenu();

        // FIXME: Move this out into a PageComplete event of sorts
        if (state == STATE_COMPLETE_FETCH) {
            fragment.setupToC(model, isFirstPage());

            //add the page to cache!
            if (cacheOnComplete) {
                app.getPageCache().put(model.getTitleOriginal(), model.getPage(),
                        new PageCache.CachePutListener() {
                            @Override
                            public void onPutComplete() {
                            }

                            @Override
                            public void onPutError(Throwable e) {
                                Log.e(TAG, "Failed to add page to cache.", e);
                            }
                        });
            }
        }
    }

    @Override
    public void setSubState(int subState) {
        this.subState = subState;
    }

    @Override
    public int getSubState() {
        return subState;
    }

    @Override
    public boolean isLoading() {
        return state != STATE_COMPLETE_FETCH;
    }

    private void loadPageOnWebViewReady(boolean tryFromCache) {
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = model.getTitle().getFragment();

        Utils.setupDirectionality(model.getTitle().getSite().getLanguage(), Locale.getDefault().getLanguage(),
                bridge);

        // hide the native top and bottom components...
        leadImagesHandler.hide();
        bottomContentHandler.hide();
        bottomContentHandler.setTitle(model.getTitle());

        if (model.getCurEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
            state = STATE_NO_FETCH;
            loadSavedPage();
        } else if (tryFromCache) {
            //is this page in cache??
            app.getPageCache()
                    .get(model.getTitleOriginal(), pageSequenceNum, new PageCache.CacheGetListener() {
                        @Override
                        public void onGetComplete(Page page, int sequence) {
                            if (sequence != pageSequenceNum) {
                                return;
                            }
                            if (page != null) {
                                Log.d(TAG, "Using page from cache: " + model.getTitleOriginal().getDisplayText());
                                model.setPage(page);
                                model.setTitle(page.getTitle());
                                // Save history entry...
                                new SaveHistoryTask(model.getCurEntry(), app).execute();
                                // don't re-cache the page after loading.
                                cacheOnComplete = false;
                                state = STATE_COMPLETE_FETCH;
                                setState(state);
                                performActionForState(state);
                            } else {
                                // page isn't in cache, so fetch it from the network...
                                loadPageFromNetwork();
                            }
                        }

                        @Override
                        public void onGetError(Throwable e, int sequence) {
                            Log.e(TAG, "Failed to get page from cache.", e);
                            if (sequence != pageSequenceNum) {
                                return;
                            }
                            // something failed when loading it from cache, so fetch it from network...
                            loadPageFromNetwork();
                        }
                    });
        } else {
            loadPageFromNetwork();
        }
    }

    private void loadPageFromNetwork() {
        state = STATE_NO_FETCH;
        // and make sure to write it to cache when it's loaded.
        cacheOnComplete = true;
        setState(state);
        performActionForState(state);
    }

    public void loadSavedPage() {
        new LoadSavedPageTask(model.getTitle()) {
            @Override
            public void onFinish(Page result) {
                // have we been unwittingly detached from our Activity?
                if (!fragment.isAdded()) {
                    Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                    return;
                }

                // Save history entry and page image url
                new SaveHistoryTask(model.getCurEntry(), app).execute();

                model.setPage(result);
                editHandler.setPage(model.getPage());
                // kick off the lead image layout
                leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                    @Override
                    public void onLayoutComplete() {
                        if (!fragment.isAdded()) {
                            return;
                        }
                        searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                        // when the lead image is laid out, load the lead section and the rest
                        // of the sections into the webview.
                        displayLeadSection();
                        displayNonLeadSection(1);

                        // rewrite the image URLs in the webview, so that they're loaded from
                        // local storage.
                        fragment.readUrlMappings();

                        setState(STATE_COMPLETE_FETCH, SUBSTATE_SAVED_PAGE_LOADED);
                    }
                });
            }

            @Override
            public void onCatch(Throwable caught) {

                /*
                If anything bad happens during loading of a saved page, then simply bounce it
                back to the online version of the page, and re-save the page contents locally when it's done.
                 */

                Log.d("LoadSavedPageTask", "Error loading saved page: " + caught.getMessage());
                caught.printStackTrace();

                fragment.refreshPage(true);
            }
        }.execute();
    }

    private boolean isFirstPage() {
        return backStack.size() <= 1 && !webView.canGoBack();
    }

    /**
     * Pop the topmost entry from the backstack.
     * Does NOT automatically load the next topmost page on the backstack.
     */
    private void popBackStack() {
        if (backStack.size() == 0) {
            return;
        }
        backStack.remove(backStack.size() - 1);
    }

    /**
     * Push the current page title onto the backstack.
     */
    private void pushBackStack() {
        PageBackStackItem item = new PageBackStackItem(model.getTitleOriginal(), model.getCurEntry());
        backStack.add(item);
    }

    /**
     * Update the current topmost backstack item, based on the currently displayed page.
     * (Things like the last y-offset position should be updated here)
     * Should be done right before loading a new page.
     */
    private void updateBackStackItem() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        item.setScrollY(webView.getScrollY());
    }

    private void loadPageFromBackStack() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.displayNewPage(item.getTitle(), item.getHistoryEntry(), true, false,
                item.getScrollY());
    }

    private void displayLeadSection() {
        try {
            final Page page = model.getPage();
            final PageProperties pageProperties = page.getPageProperties();

            JSONObject marginPayload = new JSONObject();
            int margin = DimenUtil.roundedPxToDp(activity.getResources().getDimension(R.dimen.content_margin));
            marginPayload.put("marginLeft", margin);
            marginPayload.put("marginRight", margin);
            bridge.sendMessage("setMargins", marginPayload);

            JSONObject leadSectionPayload = new JSONObject();
            leadSectionPayload.put("sequence", pageSequenceNum);
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());
            leadSectionPayload.put("string_page_similar_titles", activity.getString(R.string.page_similar_titles));
            leadSectionPayload.put("string_page_issues", activity.getString(R.string.button_page_issues));
            leadSectionPayload.put("string_table_infobox", activity.getString(R.string.table_infobox));
            leadSectionPayload.put("string_table_other", activity.getString(R.string.table_other));
            leadSectionPayload.put("string_table_close", activity.getString(R.string.table_close));
            leadSectionPayload.put("string_expand_refs", activity.getString(R.string.expand_refs));
            leadSectionPayload.put("isBeta", app.getReleaseType() != WikipediaApp.RELEASE_PROD);
            leadSectionPayload.put("siteLanguage", model.getTitle().getSite().getLanguage());
            leadSectionPayload.put("isMainPage", pageProperties.isMainPage());
            leadSectionPayload.put("apiLevel", Build.VERSION.SDK_INT);
            bridge.sendMessage("displayLeadSection", leadSectionPayload);

            Utils.setupDirectionality(model.getTitle().getSite().getLanguage(),
                    Locale.getDefault().getLanguage(), bridge);

            // Hide edit pencils if anon editing is disabled by remote killswitch or if this is a file page
            JSONObject miscPayload = new JSONObject();
            boolean isAnonEditingDisabled = app.getRemoteConfig().getConfig()
                    .optBoolean("disableAnonEditing", false)
                    && !app.getUserInfoStorage().isLoggedIn();
            miscPayload.put("noedit", (isAnonEditingDisabled
                    || model.getTitle().isFilePage()
                    || pageProperties.isMainPage()));
            miscPayload.put("protect", !pageProperties.canEdit());
            bridge.sendMessage("setPageProtected", miscPayload);
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }

        if (webView.getVisibility() != View.VISIBLE) {
            webView.setVisibility(View.VISIBLE);
        }

        refreshView.setRefreshing(false);
        activity.updateProgressBar(true, true, 0);
    }

    private void displayNonLeadSection(int index) {
        activity.updateProgressBar(true, false,
                PageActivity.PROGRESS_BAR_MAX_VALUE / model.getPage()
                        .getSections().size() * index);

        try {
            final Page page = model.getPage();
            JSONObject wrapper = new JSONObject();
            wrapper.put("sequence", pageSequenceNum);
            if (index < page.getSections().size()) {
                wrapper.put("section", page.getSections().get(index).toJSON());
                wrapper.put("index", index);
                if (sectionTargetFromIntent > 0 && sectionTargetFromIntent < page.getSections().size()) {
                    //if we have a section to scroll to (from our Intent):
                    wrapper.put("fragment",
                            page.getSections().get(sectionTargetFromIntent).getAnchor());
                } else if (sectionTargetFromTitle != null) {
                    //if we have a section to scroll to (from our PageTitle):
                    wrapper.put("fragment", sectionTargetFromTitle);
                }
            } else {
                wrapper.put("noMore", true);
            }
            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY",
                    (int) (stagedScrollY / activity.getResources().getDisplayMetrics().density));
            bridge.sendMessage("displaySection", wrapper);
        } catch (JSONException e) {
            //nope
        }
    }


    private class LeadSectionFetchTask extends SectionsFetchTask {
        public LeadSectionFetchTask(int sequenceNum) {
            super(activity, model.getTitle(), "0");
            this.sequenceNum = sequenceNum;
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            RequestBuilder builder = super.buildRequest(api);
            builder.param("prop", builder.getParams().get("prop")
                    + "|thumb|image|id|revision|description|"
                    + Page.API_REQUEST_PROPS);
            Resources res = activity.getResources();
            builder.param("thumbsize",
                    Integer.toString((int) (res.getDimension(R.dimen.leadImageWidth)
                            / res.getDisplayMetrics().density)));
            return builder;
        }

        private final int sequenceNum;
        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            if (sequenceNum != pageSequenceNum) {
                return super.processResult(result);
            }
            JSONObject mobileView = result.asObject().optJSONObject("mobileview");
            if (mobileView != null) {
                pageProperties = new PageProperties(mobileView);
                model.setTitle(fragment.adjustPageTitleFromMobileview(model.getTitle(), mobileView));
            }
            return super.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            if (!fragment.isAdded() || sequenceNum != pageSequenceNum) {
                return;
            }

            final PageTitle title = model.getTitle();
            model.setPage(new Page(title, (ArrayList<Section>) result, pageProperties));
            editHandler.setPage(model.getPage());

            // kick off the lead image layout
            leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                @Override
                public void onLayoutComplete() {
                    searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                    // when the lead image is laid out, display the lead section in the webview,
                    // and start loading the rest of the sections.
                    displayLeadSection();
                    setState(STATE_INITIAL_FETCH);
                    performActionForState(state);
                }
            });

            // Update our history entry, in case the Title was changed (i.e. normalized)
            final HistoryEntry curEntry = model.getCurEntry();
            model.setCurEntry(new HistoryEntry(title, curEntry.getTimestamp(), curEntry.getSource()));

            // Save history entry and page image url
            new SaveHistoryTask(curEntry, app).execute();

            // Fetch larger thumbnail URL for the page, to be shown in History and Saved Pages
            (new PageImagesTask(app.getAPIForSite(title.getSite()), title.getSite(),
                    Arrays.asList(new PageTitle[]{title}), WikipediaApp.PREFERRED_THUMB_SIZE) {
                @Override
                public void onFinish(Map<PageTitle, String> result) {
                    if (result.containsKey(title)) {
                        title.setThumbUrl(result.get(title));
                        PageImage pi = new PageImage(title, result.get(title));
                        app.getPersister(PageImage.class).upsert(pi);
                    }
                }

                @Override
                public void onCatch(Throwable caught) {
                    // Thumbnails are expendable
                    Log.w("SaveThumbnailTask", "Caught " + caught.getMessage(), caught);
                }
            }).execute();

        }

        @Override
        public void onCatch(Throwable caught) {
            commonSectionFetchOnCatch(caught, sequenceNum);
        }
    }

    private class RestSectionsFetchTask extends SectionsFetchTask {
        private final int sequenceNum;

        public RestSectionsFetchTask(int sequenceNum) {
            super(activity, model.getTitle(), "1-");
            this.sequenceNum = sequenceNum;
        }

        @Override
        public void onFinish(List<Section> result) {
            if (!fragment.isAdded() || sequenceNum != pageSequenceNum) {
                return;
            }

            ArrayList<Section> newSections = (ArrayList<Section>) model.getPage().getSections().clone();
            newSections.addAll(result);
            model.setPage(new Page(model.getTitle(), newSections, model.getPage().getPageProperties()));
            displayNonLeadSection(1);
            setState(STATE_COMPLETE_FETCH);

            fragment.onPageLoadComplete();
        }

        @Override
        public void onCatch(Throwable caught) {
            commonSectionFetchOnCatch(caught, sequenceNum);
        }
    }

    private void commonSectionFetchOnCatch(Throwable caught, int sequenceNum) {
        if (sequenceNum != pageSequenceNum) {
            return;
        }
        fragment.commonSectionFetchOnCatch(caught);
    }

    /**
     * Convenience method for hiding all the content of a page.
     */
    @Override
    public void onHidePageContent() {
        bottomContentHandler.hide();
    }

    @Override
    public boolean onBackPressed() {
        if (backStack.size() > 1) {
            popBackStack();
            loadPageFromBackStack();
            return true;
        }
        return false;
    }

    @Override
    public void setEditHandler(EditHandler editHandler) {
        this.editHandler = editHandler;
    }
}
