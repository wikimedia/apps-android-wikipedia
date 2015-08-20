package org.wikipedia.page;

import org.acra.ACRA;
import org.wikipedia.ApiTask;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.editing.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SaveHistoryTask;
import org.wikipedia.page.bottomcontent.BottomContentHandler;
import org.wikipedia.page.bottomcontent.BottomContentInterface;
import org.wikipedia.page.fetch.LeadSectionFetcher;
import org.wikipedia.page.fetch.LeadSectionFetcherFactory;
import org.wikipedia.page.fetch.RestSectionFetcher;
import org.wikipedia.page.fetch.RestSectionFetcherFactory;
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
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.wikipedia.page.PageFragment.SUBSTATE_NONE;
import static org.wikipedia.page.PageFragment.SUBSTATE_SAVED_PAGE_LOADED;

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
    @NonNull private List<PageBackStackItem> backStack;

    /**
     * Sequence number to maintain synchronization when loading page content asynchronously
     * between the Java and Javascript layers, as well as between async tasks and the UI thread.
     */
    private int currentSequenceNum;

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
    private PageFragment fragment;
    private CommunicationBridge bridge;
    private PageActivity activity;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikipediaApp app;
    private LeadImagesHandler leadImagesHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private EditHandler editHandler;

    private BottomContentInterface bottomContentHandler;

    JsonPageLoadStrategy() {
        backStack = new ArrayList<>();
    }

    @Override
    public void setBackStack(@NonNull List<PageBackStackItem> backStack) {
        this.backStack = backStack;
    }

    @Override
    public void setup(PageViewModel model, PageFragment fragment,
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
    public void onActivityCreated(@NonNull List<PageBackStackItem> backStack) {
        setupSpecificMessageHandlers();

        currentSequenceNum = 0;

        this.backStack = backStack;

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
                    if (messagePayload.getInt("sequence") != currentSequenceNum) {
                        return;
                    }
                    stagedScrollY = messagePayload.getInt("stagedScrollY");
                    loadPageOnWebViewReady(messagePayload.getBoolean("tryFromCache"));
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
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
                    if (messagePayload.getInt("sequence") != currentSequenceNum) {
                        return;
                    }
                    displayNonLeadSection(messagePayload.getInt("index"));
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
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
                    if (messagePayload.getInt("sequence") != currentSequenceNum) {
                        return;
                    }
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
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
            updateCurrentBackStackItem();
            pushBackStack();
        }

        state = STATE_NO_FETCH;
        subState = SUBSTATE_NONE;

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        currentSequenceNum++;

        // kick off an event to the WebView that will cause it to clear its contents,
        // and then report back to us when the clearing is complete, so that we can synchronize
        // the transitions of our native components to the new page content.
        // The callback event from the WebView will then call the loadPageOnWebViewReady()
        // function, which will continue the loading process.
        try {
            JSONObject wrapper = new JSONObject();
            // whatever we pass to this event will be passed back to us by the WebView!
            wrapper.put("sequence", currentSequenceNum);
            wrapper.put("tryFromCache", tryFromCache);
            wrapper.put("stagedScrollY", stagedScrollY);
            bridge.sendMessage("beginNewPage", wrapper);
        } catch (JSONException e) {
            ACRA.getErrorReporter().handleException(e);
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
                new LeadSectionFetchTask(currentSequenceNum).execute();
                break;
            case STATE_INITIAL_FETCH:
                new RestSectionsFetchTask(currentSequenceNum).execute();
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

        Utils.setupDirectionality(model.getTitle().getSite().getLanguageCode(), Locale.getDefault().getLanguage(),
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
            loadPageFromCache();
        } else {
            loadPageFromNetwork();
        }
    }

    private void loadPageFromCache() {
        app.getPageCache()
                .get(model.getTitleOriginal(), currentSequenceNum, new PageCache.CacheGetListener() {
                    @Override
                    public void onGetComplete(Page page, int sequence) {
                        if (sequence != currentSequenceNum) {
                            return;
                        }
                        if (page != null) {
                            Log.d(TAG, "Using page from cache: " + model.getTitleOriginal().getDisplayText());
                            model.setPage(page);
                            model.setTitle(page.getTitle());
                            // Update our history entry, in case the Title was changed (i.e. normalized)
                            final HistoryEntry curEntry = model.getCurEntry();
                            model.setCurEntry(
                                    new HistoryEntry(model.getTitle(), curEntry.getSource()));
                            // load the current title's thumbnail from sqlite
                            updateThumbnail(PageImage.PERSISTENCE_HELPER.getImageUrlForTitle(app, model.getTitle()));
                            // Save history entry...
                            new SaveHistoryTask(model.getCurEntry(), app).execute();
                            // don't re-cache the page after loading.
                            cacheOnComplete = false;
                            state = STATE_COMPLETE_FETCH;
                            setState(state);
                            performActionForState(state);
                            if (fragment.isAdded()) {
                                fragment.onPageLoadComplete();
                            }
                        } else {
                            // page isn't in cache, so fetch it from the network...
                            loadPageFromNetwork();
                        }
                    }

                    @Override
                    public void onGetError(Throwable e, int sequence) {
                        Log.e(TAG, "Failed to get page from cache.", e);
                        if (sequence != currentSequenceNum) {
                            return;
                        }
                        // something failed when loading it from cache, so fetch it from network...
                        loadPageFromNetwork();
                    }
                });
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
                    Log.d("PageFragment", "Detached from activity, so stopping update.");
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

    private void updateThumbnail(String thumbUrl) {
        model.getTitle().setThumbUrl(thumbUrl);
        model.getTitleOriginal().setThumbUrl(thumbUrl);
        fragment.invalidateTabs();
    }

    private boolean isFirstPage() {
        return backStack.size() <= 1 && !webView.canGoBack();
    }

    /**
     * Pop the topmost entry from the backstack.
     * Does NOT automatically load the next topmost page on the backstack.
     */
    private void popBackStack() {
        if (backStack.isEmpty()) {
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
    @Override
    public void updateCurrentBackStackItem() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        item.setScrollY(webView.getScrollY());
    }

    @Override
    public void loadPageFromBackStack() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.displayNewPage(item.getTitle(), item.getHistoryEntry(), true, false,
                item.getScrollY());
        Log.d(TAG, "Loaded page " + item.getTitle().getDisplayText() + " from backstack");
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
            leadSectionPayload.put("sequence", currentSequenceNum);
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());
            leadSectionPayload.put("string_page_similar_titles", activity.getString(R.string.page_similar_titles));
            leadSectionPayload.put("string_page_issues", activity.getString(R.string.button_page_issues));
            leadSectionPayload.put("string_table_infobox", activity.getString(R.string.table_infobox));
            leadSectionPayload.put("string_table_other", activity.getString(R.string.table_other));
            leadSectionPayload.put("string_table_close", activity.getString(R.string.table_close));
            leadSectionPayload.put("string_expand_refs", activity.getString(R.string.expand_refs));
            leadSectionPayload.put("isBeta", app.getReleaseType() != WikipediaApp.RELEASE_PROD);
            leadSectionPayload.put("siteLanguage", model.getTitle().getSite().getLanguageCode());
            leadSectionPayload.put("isMainPage", page.isMainPage());
            leadSectionPayload.put("apiLevel", Build.VERSION.SDK_INT);
            bridge.sendMessage("displayLeadSection", leadSectionPayload);
            Log.d(TAG, "Sent message 'displayLeadSection' for page: " + page.getDisplayTitle());

            // Hide edit pencils if anon editing is disabled by remote killswitch or if this is a file page
            JSONObject miscPayload = new JSONObject();
            boolean isAnonEditingDisabled = app.getRemoteConfig().getConfig()
                    .optBoolean("disableAnonEditing", false)
                    && !app.getUserInfoStorage().isLoggedIn();
            miscPayload.put("noedit", (isAnonEditingDisabled
                    || page.isFilePage()
                    || page.isMainPage()));
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
            wrapper.put("sequence", currentSequenceNum);
            if (index < page.getSections().size()) {
                JSONObject section = page.getSections().get(index).toJSON();
                wrapper.put("section", section);
                wrapper.put("index", index);
                if (sectionTargetFromIntent > 0 && sectionTargetFromIntent < page.getSections().size()) {
                    //if we have a section to scroll to (from our Intent):
                    wrapper.put("fragment",
                            page.getSections().get(sectionTargetFromIntent).getAnchor());
                } else if (sectionTargetFromTitle != null) {
                    //if we have a section to scroll to (from our PageTitle):
                    wrapper.put("fragment", sectionTargetFromTitle);
                } else if (model.getTitle().getFragment() != null) {
                    // It's possible, that the link was a redirect and the new title has a fragment
                    // scroll to it, if there was no fragment so far
                    wrapper.put("fragment", model.getTitle().getFragment());
                }
            } else {
                wrapper.put("noMore", true);
            }
            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY",
                    (int) (stagedScrollY / activity.getResources().getDisplayMetrics().density));
            bridge.sendMessage("displaySection", wrapper);
        } catch (JSONException e) {
            ACRA.getErrorReporter().handleException(e);
        }
    }

    private Api getAPIForSite(Site site) {
        return WikipediaApp.getInstance().getAPIForSite(site);
    }

    private class LeadSectionFetchTask extends ApiTask<List<Section>> {
        private final int startSequenceNum;
        private PageProperties pageProperties;
        private LeadSectionFetcher sectionsFetcher;

        public LeadSectionFetchTask(int startSequenceNum) {
            super(SINGLE_THREAD, getAPIForSite(model.getTitle().getSite()));
            this.sectionsFetcher = LeadSectionFetcherFactory.create(app, model.getTitle());
            this.startSequenceNum = startSequenceNum;
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            return sectionsFetcher.buildRequest(api, calculateLeadImageWidth());
        }

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            if (startSequenceNum != currentSequenceNum) {
                return sectionsFetcher.processResult(result);
            }
            JSONObject metadata
                    = result.asObject().optJSONObject(sectionsFetcher.getPagePropsResponseName());
            if (metadata != null) {
                pageProperties = new PageProperties(metadata);
                model.setTitle(fragment.adjustPageTitleFromMobileview(model.getTitle(), metadata));
            }
            return sectionsFetcher.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            if (!fragment.isAdded() || startSequenceNum != currentSequenceNum) {
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
            model.setCurEntry(
                    new HistoryEntry(title, curEntry.getTimestamp(), curEntry.getSource()));

            // Save history entry and page image url
            new SaveHistoryTask(model.getCurEntry(), app).execute();

            // Fetch larger thumbnail URL from the network, and save it to our DB.
            (new PageImagesTask(app.getAPIForSite(model.getTitle().getSite()), model.getTitle().getSite(),
                                Arrays.asList(new PageTitle[]{model.getTitle()}), WikipediaApp.PREFERRED_THUMB_SIZE) {
                @Override
                public void onFinish(Map<PageTitle, String> result) {
                    if (result.containsKey(model.getTitle())) {
                        PageImage pi = new PageImage(model.getTitle(), result.get(model.getTitle()));
                        app.getPersister(PageImage.class).upsert(pi, PageImage.PERSISTENCE_HELPER.SELECTION_KEYS);
                        updateThumbnail(result.get(model.getTitle()));
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
            commonSectionFetchOnCatch(caught, startSequenceNum);
        }
    }

    private int calculateLeadImageWidth() {
        Resources res = app.getResources();
        return (int) (res.getDimension(R.dimen.leadImageWidth) / res.getDisplayMetrics().density);
    }

    private class RestSectionsFetchTask extends ApiTask<List<Section>> {
        private final int startSequenceNum;
        private RestSectionFetcher sectionsFetcher;

        public RestSectionsFetchTask(int startSequenceNum) {
            super(SINGLE_THREAD, getAPIForSite(model.getTitle().getSite()));
            this.sectionsFetcher = RestSectionFetcherFactory.create(app, model.getTitle());
            this.startSequenceNum = startSequenceNum;
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            return sectionsFetcher.buildRequest(api);
        }

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            return sectionsFetcher.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            if (!fragment.isAdded() || startSequenceNum != currentSequenceNum) {
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
            commonSectionFetchOnCatch(caught, startSequenceNum);
        }
    }

    private void commonSectionFetchOnCatch(Throwable caught, int startSequenceNum) {
        if (startSequenceNum != currentSequenceNum) {
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
        popBackStack();
        if (!backStack.isEmpty()) {
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
