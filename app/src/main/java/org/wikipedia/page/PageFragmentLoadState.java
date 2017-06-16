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
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediawiki.api.json.ApiException;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.edit.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.login.User;
import org.wikipedia.page.bottomcontent.BottomContentHandler;
import org.wikipedia.page.bottomcontent.BottomContentInterface;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesClient;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Response;

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
public class PageFragmentLoadState {
    private interface ErrorCallback {
        void call(@Nullable Throwable error);
    }

    private boolean loading;

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

    private ErrorCallback networkErrorCallback;

    // copied fields
    private PageViewModel model;
    private PageFragment fragment;
    private CommunicationBridge bridge;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikipediaApp app = WikipediaApp.getInstance();
    private LeadImagesHandler leadImagesHandler;
    private PageToolbarHideHandler toolbarHideHandler;
    private EditHandler editHandler;

    private BottomContentInterface bottomContentHandler;

    @SuppressWarnings("checkstyle:parameternumber")
    public void setUp(@NonNull PageViewModel model,
                      @NonNull PageFragment fragment,
                      @NonNull SwipeRefreshLayoutWithScroll refreshView,
                      @NonNull ObservableWebView webView,
                      @NonNull CommunicationBridge bridge,
                      @NonNull PageToolbarHideHandler toolbarHideHandler,
                      @NonNull LeadImagesHandler leadImagesHandler,
                      @NonNull List<PageBackStackItem> backStack) {
        this.model = model;
        this.fragment = fragment;
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
        this.toolbarHideHandler = toolbarHideHandler;
        this.leadImagesHandler = leadImagesHandler;

        setUpBridgeListeners();

        bottomContentHandler = new BottomContentHandler(fragment, bridge, webView,
                fragment.getLinkHandler(),
                (ViewGroup) fragment.getView().findViewById(R.id.bottom_content_container));

        this.backStack = backStack;
    }

    public void load(boolean pushBackStack, int stagedScrollY) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem();
            pushBackStack();
        }

        loading = true;

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        sequenceNumber.increase();

        fragment.updatePageInfo(null);

        // kick off an event to the WebView that will cause it to clear its contents,
        // and then report back to us when the clearing is complete, so that we can synchronize
        // the transitions of our native components to the new page content.
        // The callback event from the WebView will then call the loadOnWebViewReady()
        // function, which will continue the loading process.
        leadImagesHandler.hide();
        bottomContentHandler.hide();
        fragment.getSearchBarHideHandler().setFadeEnabled(false);
        try {
            JSONObject wrapper = new JSONObject();
            // whatever we pass to this event will be passed back to us by the WebView!
            wrapper.put("sequence", sequenceNumber.get());
            wrapper.put("stagedScrollY", stagedScrollY);
            bridge.sendMessage("beginNewPage", wrapper);
        } catch (JSONException e) {
            L.logRemoteErrorIfProd(e);
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public void loadFromBackStack() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.loadPage(item.getTitle(), item.getHistoryEntry(), false, item.getScrollY());
        L.d("Loaded page " + item.getTitle().getDisplayText() + " from backstack");
    }

    public void updateCurrentBackStackItem() {
        if (backStack.isEmpty()) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        item.setScrollY(webView.getScrollY());
        if (model.getTitle() != null) {
            // Preserve metadata of the current PageTitle into our backstack, so that
            // this data would be available immediately upon loading PageFragment, instead
            // of only after loading the lead section.
            item.getTitle().setDescription(model.getTitle().getDescription());
            item.getTitle().setThumbUrl(model.getTitle().getThumbUrl());
        }
    }

    public void setBackStack(@NonNull List<PageBackStackItem> backStack) {
        this.backStack = backStack;
    }

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

    public boolean backStackEmpty() {
        return backStack.isEmpty();
    }

    public void onHidePageContent() {
        bottomContentHandler.hide();
    }

    public void setEditHandler(EditHandler editHandler) {
        this.editHandler = editHandler;
    }

    public void backFromEditing(Intent data) {
        //Retrieve section ID from intent, and find correct section, so where know where to scroll to
        sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
        //reset our scroll offset, since we have a section scroll target
        stagedScrollY = 0;
    }

    public void layoutLeadImage() {
        leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
            @Override
            public void onLayoutComplete(int sequence) {
                if (fragment.isAdded()) {
                    toolbarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                }
            }
        }, sequenceNumber.get());
    }

    public boolean isFirstPage() {
        return backStack.size() <= 1 && !webView.canGoBack();
    }

    @VisibleForTesting
    protected void loadLeadSection(final int startSequenceNum) {
        app.getSessionFunnel().leadSectionFetchStart();
        PageClientFactory
                .create(model.getTitle().getWikiSite(), model.getTitle().namespace())
                .lead(null, PageClient.CacheOption.CACHE, model.getTitle().getPrefixedText(),
                        calculateLeadImageWidth(), !app.isImageDownloadEnabled())
                .enqueue(new retrofit2.Callback<PageLead>() {
                    @Override public void onResponse(Call<PageLead> call, Response<PageLead> rsp) {
                        app.getSessionFunnel().leadSectionFetchEnd();
                        PageLead lead = rsp.body();
                        onLeadSectionLoaded(lead, startSequenceNum);
                        if (rsp.raw().cacheResponse() != null) {
                            showPageOfflineMessage(rsp.raw().header("date", ""));
                        }
                    }

                    @Override public void onFailure(Call<PageLead> call, Throwable t) {
                        L.e("PageLead error: ", t);
                        commonSectionFetchOnCatch(t, startSequenceNum);
                    }
                });
    }

    @VisibleForTesting
    protected void commonSectionFetchOnCatch(Throwable caught, int startSequenceNum) {
        ErrorCallback callback = networkErrorCallback;
        networkErrorCallback = null;
        loading = false;
        if (fragment.callback() != null) {
            fragment.callback().onPageInvalidateOptionsMenu();
        }
        if (!sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        if (callback != null) {
            callback.call(caught);
        }
    }

    private void setUpBridgeListeners() {
        bridge.addListener("onBeginNewPage", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                try {
                    stagedScrollY = payload.getInt("stagedScrollY");
                    loadOnWebViewReady();
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("requestSection", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                try {
                    displayNonLeadSection(payload.getInt("index"));
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("pageLoadComplete", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                // Do any other stuff that should happen upon page load completion...
                if (fragment.callback() != null) {
                    fragment.callback().onPageUpdateProgressBar(false, true, 0);
                }

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
                            model.getTitle().getWikiSite(), payload);
                    fragment.updatePageInfo(pageInfo);
                }
            }
        });
    }

    private void loadOnWebViewReady() {
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = model.getTitle().getFragment();

        L10nUtil.setupDirectionality(model.getTitle().getWikiSite().languageCode(), Locale.getDefault().getLanguage(),
                bridge);

        loadFromNetwork(new ErrorCallback() {
            @Override public void call(final Throwable networkError) {
                fragment.onPageLoadError(networkError);
            }
        });
    }

    private void loadFromNetwork(final ErrorCallback errorCallback) {
        networkErrorCallback = errorCallback;
        if (!fragment.isAdded()) {
            return;
        }
        loading = true;
        if (fragment.callback() != null) {
            fragment.callback().onPageInvalidateOptionsMenu();
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
        loadLeadSection(sequenceNumber.get());
    }

    private void updateThumbnail(String thumbUrl) {
        model.getTitle().setThumbUrl(thumbUrl);
        model.getTitleOriginal().setThumbUrl(thumbUrl);
        fragment.invalidateTabs();
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
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
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
                    .put("isBeta", ReleaseUtil.isPreProdRelease()) // True for any non-production release type
                    .put("siteLanguage", model.getTitle().getWikiSite().languageCode())
                    .put("siteBaseUrl", model.getTitle().getWikiSite().url())
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
                ResourceUtil.getIdArray(fragment.getContext(), R.array.page_localized_string_ids));
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
        return (User.isLoggedIn() || !isAnonEditingDisabled())
                && !page.isFilePage()
                && !page.isMainPage();
    }

    private boolean isAnonEditingDisabled() {
        return getRemoteConfig().optBoolean("disableAnonEditing", false);
    }

    private void showPageOfflineMessage(@NonNull String dateHeader) {
        if (!fragment.isAdded()) {
            return;
        }
        try {
            String dateStr = DateUtil.getShortDateString(DateUtil
                    .getHttpLastModifiedDate(dateHeader));
            Toast.makeText(fragment.getContext().getApplicationContext(),
                    fragment.getString(R.string.page_offline_notice_last_date, dateStr),
                    Toast.LENGTH_LONG).show();
        } catch (ParseException e) {
            // ignore
        }
    }

    private JSONObject getRemoteConfig() {
        return app.getRemoteConfig().getConfig();
    }

    private void displayNonLeadSection(int index) {
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, false,
                    Constants.PROGRESS_BAR_MAX_VALUE / model.getPage()
                            .getSections().size() * index);
        }
        try {
            final Page page = model.getPage();
            JSONObject wrapper = new JSONObject();
            wrapper.put("sequence", sequenceNumber.get());
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

        if (page.getTitle().getDescription() == null) {
            app.getSessionFunnel().noDescription();
        }

        layoutLeadImage(new Runnable() {
            @Override
            public void run() {
                if (!fragment.isAdded()) {
                    return;
                }
                fragment.callback().onPageInvalidateOptionsMenu();
                loadRemainingSections(sequenceNumber.get());
            }
        });

        // Update our history entry, in case the Title was changed (i.e. normalized)
        final HistoryEntry curEntry = model.getCurEntry();
        model.setCurEntry(
                new HistoryEntry(model.getTitle(), curEntry.getTimestamp(), curEntry.getSource()));

        // Fetch larger thumbnail URL from the network, and save it to our DB.
        new PageImagesClient().request(model.getTitle().getWikiSite(), Collections.singletonList(model.getTitle()),
                new PageImagesClient.Callback() {
                    @Override public void success(@NonNull Call<MwQueryResponse> call,
                                                  @NonNull Map<PageTitle, PageImage> results) {
                        if (results.containsKey(model.getTitle())) {
                            PageImage pageImage = results.get(model.getTitle());
                            app.getDatabaseClient(PageImage.class)
                                    .upsert(pageImage, PageImageHistoryContract.Image.SELECTION);
                            updateThumbnail(pageImage.getImageName());
                        }
                    }
                    @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                                  @NonNull Throwable caught) {
                        L.w(caught);
                    }
                });
    }

    private void loadRemainingSections(final int startSequenceNum) {
        app.getSessionFunnel().restSectionsFetchStart();
        PageClientFactory
                .create(model.getTitle().getWikiSite(), model.getTitle().namespace())
                .sections(null, PageClient.CacheOption.CACHE, model.getTitle().getPrefixedText(),
                        !app.isImageDownloadEnabled())
                .enqueue(new retrofit2.Callback<PageRemaining>() {
                    @Override public void onResponse(Call<PageRemaining> call, Response<PageRemaining> rsp) {
                        app.getSessionFunnel().restSectionsFetchEnd();
                        PageRemaining sections = rsp.body();
                        onRemainingSectionsLoaded(sections, startSequenceNum);
                    }

                    @Override public void onFailure(Call<PageRemaining> call, Throwable t) {
                        L.e("PageRemaining error: ", t);
                        commonSectionFetchOnCatch(t, startSequenceNum);
                    }
                });
    }

    private void onRemainingSectionsLoaded(PageRemaining pageRemaining, int startSequenceNum) {
        networkErrorCallback = null;
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }

        pageRemaining.mergeInto(model.getPage());

        displayNonLeadSection(1);
        loading = false;
        fragment.onPageLoadComplete();
    }

    private float getDimension(@DimenRes int id) {
        return getResources().getDimension(id);
    }

    private Resources getResources() {
        return fragment.getResources();
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
            toolbarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());

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
