package org.wikipedia.page;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.ApiException;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.MainActivity;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.editing.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SaveHistoryTask;
import org.wikipedia.page.bottomcontent.BottomContentHandler;
import org.wikipedia.page.bottomcontent.BottomContentInterface;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageRemaining;
import org.wikipedia.server.PageServiceFactory;
import org.wikipedia.server.ServiceError;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.wikipedia.util.DimenUtil.calculateLeadImageWidth;
import static org.wikipedia.util.L10nUtil.getStringsForArticleLanguage;

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
    private interface ErrorCallback {
        void call(@Nullable Throwable error);
    }

    private static final String BRIDGE_PAYLOAD_SAVED_PAGE = "savedPage";

    private static final int STATE_NO_FETCH = 1;
    private static final int STATE_INITIAL_FETCH = 2;
    private static final int STATE_COMPLETE_FETCH = 3;

    private int state = STATE_NO_FETCH;

    /**
     * List of lightweight history items to serve as the backstack for this fragment.
     * Since the list consists of Parcelable objects, it can be saved and restored from the
     * savedInstanceState of the fragment.
     */
    @NonNull private List<PageBackStackItem> backStack = new ArrayList<>();

    @NonNull private final SequenceNumber sequenceNumber = new SequenceNumber();

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

    private ErrorCallback networkErrorCallback;

    // copied fields
    private PageViewModel model;
    private PageFragment fragment;
    private CommunicationBridge bridge;
    private MainActivity activity;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    @NonNull private final WikipediaApp app = WikipediaApp.getInstance();
    private LeadImagesHandler leadImagesHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private EditHandler editHandler;

    private BottomContentInterface bottomContentHandler;

    @Override
    @SuppressWarnings("checkstyle:parameternumber")
    public void setUp(@NonNull PageViewModel model,
                      @NonNull PageFragment fragment,
                      @NonNull SwipeRefreshLayoutWithScroll refreshView,
                      @NonNull ObservableWebView webView,
                      @NonNull CommunicationBridge bridge,
                      @NonNull SearchBarHideHandler searchBarHideHandler,
                      @NonNull LeadImagesHandler leadImagesHandler,
                      @NonNull List<PageBackStackItem> backStack) {
        this.model = model;
        this.fragment = fragment;
        activity = (MainActivity) fragment.getActivity();
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
        this.searchBarHideHandler = searchBarHideHandler;
        this.leadImagesHandler = leadImagesHandler;

        setUpBridgeListeners();

        bottomContentHandler = new BottomContentHandler(fragment, bridge, webView,
                fragment.getLinkHandler(),
                (ViewGroup) fragment.getView().findViewById(R.id.bottom_content_container));

        this.backStack = backStack;

        // if we already have pages in the backstack (whether it's from savedInstanceState, or
        // from being stored in the activity's fragment backstack), then load the topmost page
        // on the backstack.
        loadFromBackStack();
    }

    @Override
    public void load(boolean pushBackStack, @NonNull Cache cachePreference, int stagedScrollY) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem();
            pushBackStack();
        }

        state = STATE_NO_FETCH;

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        sequenceNumber.increase();

        if (cachePreference == Cache.NONE) {
            // If this is a refresh, don't clear the webview contents
            this.stagedScrollY = stagedScrollY;
            loadOnWebViewReady(cachePreference);
        } else {
            fragment.updatePageInfo(null);
            leadImagesHandler.updateNavigate(null);

            // kick off an event to the WebView that will cause it to clear its contents,
            // and then report back to us when the clearing is complete, so that we can synchronize
            // the transitions of our native components to the new page content.
            // The callback event from the WebView will then call the loadOnWebViewReady()
            // function, which will continue the loading process.
            leadImagesHandler.hide();
            bottomContentHandler.hide();
            activity.getSearchBarHideHandler().setFadeEnabled(false);
            try {
                JSONObject wrapper = new JSONObject();
                // whatever we pass to this event will be passed back to us by the WebView!
                wrapper.put("sequence", sequenceNumber.get());
                wrapper.put("cachePreference", cachePreference.name());
                wrapper.put("stagedScrollY", stagedScrollY);
                bridge.sendMessage("beginNewPage", wrapper);
            } catch (JSONException e) {
                L.logRemoteErrorIfProd(e);
            }
        }
    }

    @Override
    public boolean isLoading() {
        return state != STATE_COMPLETE_FETCH;
    }

    @Override
    public void loadFromBackStack() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.loadPage(item.getTitle(), item.getHistoryEntry(), Cache.PREFERRED, false,
                item.getScrollY());
        L.d("Loaded page " + item.getTitle().getDisplayText() + " from backstack");
    }

    @Override
    public void updateCurrentBackStackItem() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        item.setScrollY(webView.getScrollY());
    }

    @Override
    public void setBackStack(@NonNull List<PageBackStackItem> backStack) {
        this.backStack = backStack;
    }

    @Override
    public boolean popBackStack() {
        if (!backStack.isEmpty()) {
            backStack.remove(backStack.size() - 1);
        }

        if (!backStack.isEmpty()) {
            loadFromBackStack();
            return true;
        }

        return false;
    }

    @Override public boolean backStackEmpty() {
        return backStack.isEmpty();
    }

    @Override
    public void onHidePageContent() {
        bottomContentHandler.hide();
    }

    @Override
    public void setEditHandler(EditHandler editHandler) {
        this.editHandler = editHandler;
    }

    @Override
    public void backFromEditing(Intent data) {
        //Retrieve section ID from intent, and find correct section, so where know where to scroll to
        sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
        //reset our scroll offset, since we have a section scroll target
        stagedScrollY = 0;
    }

    @Override
    public void layoutLeadImage() {
        leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
            @Override
            public void onLayoutComplete(int sequence) {
                if (fragment.isAdded()) {
                    searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                }
            }
        }, sequenceNumber.get());
    }

    @VisibleForTesting
    protected void loadLeadSection(final int startSequenceNum) {
        app.getSessionFunnel().leadSectionFetchStart();
        PageServiceFactory.create(model.getTitle().getSite()).pageLead(
                model.getTitle().getPrefixedText(),
                calculateLeadImageWidth(),
                !app.isImageDownloadEnabled(),
                new PageLead.Callback() {
                    @Override
                    public void success(PageLead pageLead) {
                        app.getSessionFunnel().leadSectionFetchEnd();
                        onLeadSectionLoaded(pageLead, startSequenceNum);
                    }

                    @Override
                    public void failure(Throwable error) {
                        L.e("PageLead error: ", error);
                        commonSectionFetchOnCatch(error, startSequenceNum);
                    }
                });
    }

    @VisibleForTesting
    protected void commonSectionFetchOnCatch(Throwable caught, int startSequenceNum) {
        ErrorCallback callback = networkErrorCallback;
        networkErrorCallback = null;
        cacheOnComplete = false;
        state = STATE_COMPLETE_FETCH;
        activity.supportInvalidateOptionsMenu();
        if (!sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        if (callback != null) {
            callback.call(caught);
        }
    }

    private void loadSavedPage(final ErrorCallback errorCallback) {
        new LoadSavedPageTask(model.getTitle(), sequenceNumber.get()) {
            @Override
            public void onFinish(Page result) {
                if (!fragment.isAdded() || !sequenceNumber.inSync(getSequence())) {
                    return;
                }

                // Save history entry and page image url
                new SaveHistoryTask(model.getCurEntry(), app).execute();

                model.setPage(result);
                editHandler.setPage(model.getPage());
                layoutLeadImage(new Runnable() {
                    @Override
                    public void run() {
                        displayNonLeadSectionForSavedPage(1);
                        setState(STATE_COMPLETE_FETCH);
                    }
                });
            }

            @Override
            public void onCatch(Throwable caught) {
                errorCallback.call(caught);
            }
        }.execute();
    }

    private void setUpBridgeListeners() {
        bridge.addListener("onBeginNewPage", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                try {
                    stagedScrollY = payload.getInt("stagedScrollY");
                    loadOnWebViewReady(Cache.valueOf(payload.getString("cachePreference")));
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("requestSection", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                try {
                    displayNonLeadSection(payload.getInt("index"),
                            payload.optBoolean(BRIDGE_PAYLOAD_SAVED_PAGE, false));
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("pageLoadComplete", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                // Do any other stuff that should happen upon page load completion...
                activity.updateProgressBar(false, true, 0);

                // trigger layout of the bottom content
                // Check to see if the page title has changed (e.g. due to following a redirect),
                // because if it has then the handler needs the new title to make sure it doesn't
                // accidentally display the current article as a "read more" suggestion
                bottomContentHandler.setTitle(model.getTitle());
                bottomContentHandler.beginLayout();
            }
        });
        bridge.addListener("pageInfo", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String message, JSONObject payload) {
                if (fragment.isAdded()) {
                    PageInfo pageInfo = PageInfoUnmarshaller.unmarshal(model.getTitle(),
                            model.getTitle().getSite(), payload);
                    fragment.updatePageInfo(pageInfo);
                }
            }
        });
    }

    private void performActionForState(int forState) {
        if (!fragment.isAdded()) {
            return;
        }
        switch (forState) {
            case STATE_NO_FETCH:
                activity.updateProgressBar(true, true, 0);
                loadLeadSection(sequenceNumber.get());
                break;
            case STATE_INITIAL_FETCH:
                loadRemainingSections(sequenceNumber.get());
                break;
            case STATE_COMPLETE_FETCH:
                editHandler.setPage(model.getPage());
                layoutLeadImage(new Runnable() {
                    @Override
                    public void run() {
                        displayNonLeadSectionForUnsavedPage(1);
                    }
                });
                break;
            default:
                throw new RuntimeException("Unknown state encountered " + state);
        }
    }

    private void setState(int state) {
        if (!fragment.isAdded()) {
            return;
        }
        this.state = state;
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
                                L.e("Failed to add page to cache.", e);
                            }
                        });
            }
        }
    }

    private void loadOnWebViewReady(Cache cachePreference) {
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = model.getTitle().getFragment();

        L10nUtil.setupDirectionality(model.getTitle().getSite().languageCode(), Locale.getDefault().getLanguage(),
                bridge);

        switch (cachePreference) {
            case PREFERRED:
                loadFromCache(new ErrorCallback() {
                    @Override
                    public void call(Throwable cacheError) {
                        loadFromNetwork(new ErrorCallback() {
                            @Override
                            public void call(final Throwable networkError) {
                                loadSavedPage(new ErrorCallback() {
                                    @Override
                                    public void call(Throwable savedError) {
                                        fragment.onPageLoadError(networkError);
                                    }
                                });
                            }
                        });
                    }
                });
                break;
            case FALLBACK:
                loadFromNetwork(new ErrorCallback() {
                    @Override
                    public void call(final Throwable networkError) {
                        loadFromCache(new ErrorCallback() {
                            @Override
                            public void call(Throwable cacheError) {
                                loadSavedPage(new ErrorCallback() {
                                    @Override
                                    public void call(Throwable savedError) {
                                        fragment.onPageLoadError(networkError);
                                    }
                                });
                            }
                        });
                    }
                });
                break;
            case NONE:
            default:
                // This is a refresh, don't clear contents in this case
                loadFromNetwork(new ErrorCallback() {
                    @Override
                    public void call(Throwable networkError) {
                        fragment.onPageLoadError(networkError);
                    }
                });
                break;
        }
    }

    private void loadFromCache(final ErrorCallback errorCallback) {
        app.getPageCache()
                .get(model.getTitleOriginal(), sequenceNumber.get(), new PageCache.CacheGetListener() {
                    @Override
                    public void onGetComplete(Page page, int sequence) {
                        if (!sequenceNumber.inSync(sequence)) {
                            return;
                        }
                        if (page != null) {
                            L.d("Using page from cache: " + model.getTitleOriginal().getDisplayText());
                            model.setPage(page);
                            model.setTitle(page.getTitle());
                            // Update our history entry, in case the Title was changed (i.e. normalized)
                            final HistoryEntry curEntry = model.getCurEntry();
                            model.setCurEntry(
                                    new HistoryEntry(model.getTitle(), curEntry.getSource()));
                            // load the current title's thumbnail from sqlite
                            updateThumbnail(PageImage.DATABASE_TABLE.getImageUrlForTitle(app, model.getTitle()));
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
                            errorCallback.call(null);
                        }
                    }

                    @Override
                    public void onGetError(Throwable e, int sequence) {
                        L.e("Failed to get page from cache.", e);
                        if (!sequenceNumber.inSync(sequence)) {
                            return;
                        }
                        errorCallback.call(e);
                    }
                });
    }

    private void loadFromNetwork(final ErrorCallback errorCallback) {
        networkErrorCallback = errorCallback;
        // and make sure to write it to cache when it's loaded.
        cacheOnComplete = true;
        setState(STATE_NO_FETCH);
        performActionForState(state);
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
     * Push the current page title onto the backstack.
     */
    private void pushBackStack() {
        PageBackStackItem item = new PageBackStackItem(model.getTitleOriginal(), model.getCurEntry());
        backStack.add(item);
    }

    private void layoutLeadImage(@Nullable Runnable runnable) {
        leadImagesHandler.beginLayout(new LeadImageLayoutListener(runnable), sequenceNumber.get());
    }

    private void displayLeadSection() {
        Page page = model.getPage();

        sendMarginPayload();

        sendLeadSectionPayload(page);

        sendMiscPayload(page);

        if (webView.getVisibility() != View.VISIBLE) {
            webView.setVisibility(View.VISIBLE);
        }

        refreshView.setRefreshing(false);
        activity.updateProgressBar(true, true, 0);

        leadImagesHandler.updateNavigate(page.getPageProperties().getGeo());
    }

    private void sendMarginPayload() {
        JSONObject marginPayload = marginPayload();
        bridge.sendMessage("setMargins", marginPayload);
    }

    private JSONObject marginPayload() {
        int horizontalMargin = DimenUtil.roundedPxToDp(getDimension(R.dimen.content_margin));
        int verticalMargin = DimenUtil.roundedPxToDp(getDimension(R.dimen.activity_vertical_margin));
        try {
            return new JSONObject()
                    .put("marginTop", verticalMargin)
                    .put("marginLeft", horizontalMargin)
                    .put("marginRight", horizontalMargin);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendLeadSectionPayload(Page page) {
        JSONObject leadSectionPayload = leadSectionPayload(page);
        bridge.sendMessage("displayLeadSection", leadSectionPayload);
        L.d("Sent message 'displayLeadSection' for page: " + page.getDisplayTitle());
    }

    private JSONObject leadSectionPayload(Page page) {
        SparseArray<String> localizedStrings = localizedStrings(page);

        try {
            return new JSONObject()
                    .put("sequence", sequenceNumber.get())
                    .put("title", page.getDisplayTitle())
                    .put("section", page.getSections().get(0).toJSON())
                    .put("string_table_infobox", localizedStrings.get(R.string.table_infobox))
                    .put("string_table_other", localizedStrings.get(R.string.table_other))
                    .put("string_table_close", localizedStrings.get(R.string.table_close))
                    .put("string_expand_refs", localizedStrings.get(R.string.expand_refs))
                    .put("isBeta", app.isPreProdRelease()) // True for any non-production release type
                    .put("siteLanguage", model.getTitle().getSite().languageCode())
                    .put("isMainPage", page.isMainPage())
                    .put("fromRestBase", page.isFromRestBase())
                    .put("isNetworkMetered", DeviceUtil.isNetworkMetered(app))
                    .put("apiLevel", Build.VERSION.SDK_INT);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private SparseArray<String> localizedStrings(Page page) {
        return getStringsForArticleLanguage(page.getTitle(),
                ResourceUtil.getIdArray(activity, R.array.page_localized_string_ids));
    }


    private void sendMiscPayload(Page page) {
        JSONObject miscPayload = miscPayload(page);
        bridge.sendMessage("setPageProtected", miscPayload);
    }

    private JSONObject miscPayload(Page page) {
        try {
            return new JSONObject()
                    .put("noedit", !isPageEditable(page)) // Controls whether edit pencils are visible.
                    .put("protect", page.isProtected());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isPageEditable(Page page) {
        return (app.getUserInfoStorage().isLoggedIn() || !isAnonEditingDisabled())
                && !page.isFilePage()
                && !page.isMainPage();
    }

    private boolean isAnonEditingDisabled() {
        return getRemoteConfig().optBoolean("disableAnonEditing", false);
    }

    private JSONObject getRemoteConfig() {
        return app.getRemoteConfig().getConfig();
    }

    private void displayNonLeadSectionForUnsavedPage(int index) {
        displayNonLeadSection(index, false);
    }

    private void displayNonLeadSectionForSavedPage(int index) {
        displayNonLeadSection(index, true);
    }

    private void displayNonLeadSection(int index, boolean savedPage) {
        activity.updateProgressBar(true, false,
                MainActivity.PROGRESS_BAR_MAX_VALUE / model.getPage()
                        .getSections().size() * index);
        try {
            final Page page = model.getPage();
            JSONObject wrapper = new JSONObject();
            wrapper.put("sequence", sequenceNumber.get());
            wrapper.put(BRIDGE_PAYLOAD_SAVED_PAGE, savedPage);
            boolean lastSection = index == page.getSections().size();
            if (!lastSection) {
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
                } else if (!TextUtils.isEmpty(model.getTitle().getFragment())) {
                    // It's possible, that the link was a redirect and the new title has a fragment
                    // scroll to it, if there was no fragment so far
                    wrapper.put("fragment", model.getTitle().getFragment());
                }
            } else {
                wrapper.put("noMore", true);
            }
            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY",
                    (int) (stagedScrollY / DimenUtil.getDensityScalar()));
            bridge.sendMessage("displaySection", wrapper);

            if (savedPage && lastSection) {
                // rewrite the image URLs in the webview, so that they're loaded from
                // local storage after all the sections have been loaded.
                fragment.readUrlMappings();
            }
        } catch (JSONException e) {
            L.logRemoteErrorIfProd(e);
        }
    }

    private void onLeadSectionLoaded(PageLead pageLead, int startSequenceNum) {
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        if (pageLead.hasError()) {
            ServiceError error = pageLead.getError();
            if (error != null) {
                ApiException apiException = new ApiException(error.getTitle(), error.getDetails());
                commonSectionFetchOnCatch(apiException, startSequenceNum);
            } else {
                ApiException apiException
                        = new ApiException("unknown", "unexpected pageLead response");
                commonSectionFetchOnCatch(apiException, startSequenceNum);
            }
            return;
        }

        Page page = pageLead.toPage(model.getTitle());
        model.setPage(page);
        model.setTitle(page.getTitle());

        editHandler.setPage(model.getPage());

        layoutLeadImage(new Runnable() {
            @Override
            public void run() {
                setState(STATE_INITIAL_FETCH);
                performActionForState(state);
            }
        });

        // Update our history entry, in case the Title was changed (i.e. normalized)
        final HistoryEntry curEntry = model.getCurEntry();
        model.setCurEntry(
                new HistoryEntry(model.getTitle(), curEntry.getTimestamp(), curEntry.getSource()));

        // Save history entry and page image url
        new SaveHistoryTask(model.getCurEntry(), app).execute();

        // Fetch larger thumbnail URL from the network, and save it to our DB.
        (new PageImagesTask(app.getAPIForSite(model.getTitle().getSite()), model.getTitle().getSite(),
                Arrays.asList(new PageTitle[]{model.getTitle()}), Constants.PREFERRED_THUMB_SIZE) {
            @Override
            public void onFinish(Map<PageTitle, String> result) {
                if (result.containsKey(model.getTitle())) {
                    PageImage pi = new PageImage(model.getTitle(), result.get(model.getTitle()));
                    app.getDatabaseClient(PageImage.class).upsert(pi, PageImageHistoryContract.Image.SELECTION);
                    updateThumbnail(result.get(model.getTitle()));
                }
            }

            @Override
            public void onCatch(Throwable caught) {
                L.w(caught);
            }
        }).execute();
    }

    private void loadRemainingSections(final int startSequenceNum) {
        app.getSessionFunnel().restSectionsFetchStart();
        PageServiceFactory.create(model.getTitle().getSite()).pageRemaining(
                model.getTitle().getPrefixedText(),
                !app.isImageDownloadEnabled(),
                new PageRemaining.Callback() {
                    @Override
                    public void success(PageRemaining pageRemaining) {
                        app.getSessionFunnel().restSectionsFetchEnd();
                        onRemainingSectionsLoaded(pageRemaining, startSequenceNum);
                    }

                    @Override
                    public void failure(Throwable throwable) {
                        L.e("PageRemaining error: ", throwable);
                        commonSectionFetchOnCatch(throwable, startSequenceNum);
                    }
                });
    }

    private void onRemainingSectionsLoaded(PageRemaining pageRemaining, int startSequenceNum) {
        networkErrorCallback = null;
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }

        pageRemaining.mergeInto(model.getPage());

        displayNonLeadSectionForUnsavedPage(1);
        setState(STATE_COMPLETE_FETCH);

        fragment.onPageLoadComplete();
    }

    private float getDimension(@DimenRes int id) {
        return getResources().getDimension(id);
    }

    private Resources getResources() {
        return activity.getResources();
    }

    private class LeadImageLayoutListener implements LeadImagesHandler.OnLeadImageLayoutListener {
        @Nullable private final Runnable runnable;

        LeadImageLayoutListener(@Nullable Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void onLayoutComplete(int sequence) {
            if (!fragment.isAdded() || !sequenceNumber.inSync(sequence)) {
                return;
            }
            searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());

            if (runnable != null) {
                // when the lead image is laid out, load the lead section and the rest
                // of the sections into the webview.
                displayLeadSection();
                runnable.run();
            }
        }
    }

    private abstract class SynchronousBridgeListener implements CommunicationBridge.JSEventListener {
        private static final String BRIDGE_PAYLOAD_SEQUENCE = "sequence";

        @Override
        public void onMessage(String message, JSONObject payload) {
            if (fragment.isAdded() && inSync(payload)) {
                onMessage(payload);
            }
        }

        protected abstract void onMessage(JSONObject payload);

        private boolean inSync(JSONObject payload) {
            return sequenceNumber.inSync(payload.optInt(BRIDGE_PAYLOAD_SEQUENCE,
                    sequenceNumber.get() - 1));
        }
    }

    /**
     * Monotonically increasing sequence number to maintain synchronization when loading page
     * content asynchronously between the Java and JavaScript layers, as well as between synchronous
     * methods and asynchronous callbacks on the UI thread.
     */
    private static class SequenceNumber {
        private int sequence;

        void increase() {
            ++sequence;
        }

        int get() {
            return sequence;
        }

        boolean inSync(int sequence) {
            return this.sequence == sequence;
        }
    }
}
