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
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.edit.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.offline.OfflineContentProvider;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesClient;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.CacheControl;
import okhttp3.Protocol;
import okhttp3.Request;
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
    private EditHandler editHandler;

    @SuppressWarnings("checkstyle:parameternumber")
    public void setUp(@NonNull PageViewModel model,
                      @NonNull PageFragment fragment,
                      @NonNull SwipeRefreshLayoutWithScroll refreshView,
                      @NonNull ObservableWebView webView,
                      @NonNull CommunicationBridge bridge,
                      @NonNull LeadImagesHandler leadImagesHandler,
                      @NonNull List<PageBackStackItem> backStack) {
        this.model = model;
        this.fragment = fragment;
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
        this.leadImagesHandler = leadImagesHandler;

        setUpBridgeListeners();

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

        this.stagedScrollY = stagedScrollY;
        pageLoadCheckReadingLists(sequenceNumber.get());
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
        leadImagesHandler.beginLayout((sequence) -> {
            if (fragment.isAdded()) {
                fragment.setToolbarFadeEnabled(leadImagesHandler.isLeadImageEnabled());
            }
        }, sequenceNumber.get());
    }

    public boolean isFirstPage() {
        return backStack.size() <= 1 && !webView.canGoBack();
    }

    @VisibleForTesting
    protected void commonSectionFetchOnCatch(Throwable caught, int startSequenceNum) {
        if (!sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        ErrorCallback callback = networkErrorCallback;
        networkErrorCallback = null;
        loading = false;
        if (fragment.callback() != null) {
            fragment.callback().onPageInvalidateOptionsMenu();
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
                    if (!sequenceNumber.inSync(payload.getInt("sequence"))) {
                        return;
                    }
                    pageLoadWebViewReady();
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("loadRemainingError", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                try {
                    if (!sequenceNumber.inSync(payload.getInt("sequence"))) {
                        return;
                    }
                    int sequence = payload.getInt("sequence");
                    int status = payload.getInt("status");
                    commonSectionFetchOnCatch(new HttpStatusException(new okhttp3.Response.Builder()
                            .code(status).protocol(Protocol.HTTP_1_1).message("")
                            .request(new okhttp3.Request.Builder()
                                    .url(model.getTitle().getMobileUri()).build()).build()),
                            sequence);
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("pageLoadComplete", new SynchronousBridgeListener() {
            @Override
            public void onMessage(JSONObject payload) {
                app.getSessionFunnel().restSectionsFetchEnd();

                if (fragment.callback() != null) {
                    fragment.callback().onPageUpdateProgressBar(false, true, 0);
                }

                try {
                    if (!sequenceNumber.inSync(payload.getInt("sequence"))) {
                        return;
                    }
                    if (payload.has("sections")) {
                        // augment our current Page object with updated Sections received from JS
                        List<Section> sectionList = new ArrayList<>();
                        JSONArray sections = payload.getJSONArray("sections");
                        for (int i = 0; i < sections.length(); i++) {
                            JSONObject s = sections.getJSONObject(i);
                            sectionList.add(new Section(s.getInt("id"),
                                    s.getInt("toclevel") - 1,
                                    s.getString("line"),
                                    s.getString("anchor"),
                                    ""));
                        }
                        Page page = model.getPage();
                        page.getSections().addAll(sectionList);
                    }
                } catch (JSONException e) {
                    L.e(e);
                }

                loading = false;
                networkErrorCallback = null;
                fragment.onPageLoadComplete();
            }
        });
        bridge.addListener("pageInfo", (String message, JSONObject payload) -> {
            if (fragment.isAdded()) {
                PageInfo pageInfo = PageInfoUnmarshaller.unmarshal(model.getTitle(),
                        model.getTitle().getWikiSite(), payload);
                fragment.updatePageInfo(pageInfo);
            }
        });
    }

    private void pageLoadCheckReadingLists(final int sequence) {
        CallbackTask.execute(() -> ReadingListDbHelper.instance().findPageInAnyList(model.getTitle()), new CallbackTask.Callback<ReadingListPage>() {
            @Override
            public void success(ReadingListPage page) {
                if (!sequenceNumber.inSync(sequence)) {
                    return;
                }
                model.setReadingListPage(page);
                fragment.updateBookmarkAndMenuOptions();
                pageLoadPrepareWebView();
            }
            @Override
            public void failure(Throwable caught) {
                if (!sequenceNumber.inSync(sequence)) {
                    return;
                }
                L.w(caught);
                fragment.updateBookmarkAndMenuOptions();
                pageLoadPrepareWebView();
            }
        });
    }

    private void pageLoadPrepareWebView() {
        try {
            JSONObject wrapper = new JSONObject();
            // whatever we pass to this event will be passed back to us by the WebView!
            wrapper.put("sequence", sequenceNumber.get());
            bridge.sendMessage("beginNewPage", wrapper);
        } catch (JSONException e) {
            L.logRemoteErrorIfProd(e);
        }
    }

    private void pageLoadWebViewReady() {
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = model.getTitle().getFragment();

        L10nUtil.setupDirectionality(model.getTitle().getWikiSite().languageCode(), Locale.getDefault().getLanguage(),
                bridge);

        if (Prefs.preferOfflineContent() && OfflineManager.instance().titleExists(model.getTitle().getDisplayText())) {
            pageLoadFromCompilation();
        } else {
            pageLoadFromNetwork((final Throwable networkError) -> fragment.onPageLoadError(networkError));
        }
    }

    private void pageLoadFromNetwork(final ErrorCallback errorCallback) {
        networkErrorCallback = errorCallback;
        if (!fragment.isAdded()) {
            return;
        }
        loading = true;
        if (fragment.callback() != null) {
            fragment.callback().onPageInvalidateOptionsMenu();
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
        pageLoadLeadSection(sequenceNumber.get());
    }

    @VisibleForTesting
    protected void pageLoadLeadSection(final int startSequenceNum) {
        app.getSessionFunnel().leadSectionFetchStart();
        PageClientFactory
                .create(model.getTitle().getWikiSite(), model.getTitle().namespace())
                .lead(model.shouldForceNetwork() ? CacheControl.FORCE_NETWORK : null,
                        model.shouldSaveOffline() ? PageClient.CacheOption.SAVE : PageClient.CacheOption.CACHE,
                        model.getTitle().getPrefixedText(), calculateLeadImageWidth())
                .enqueue(new retrofit2.Callback<PageLead>() {
                    @Override public void onResponse(@NonNull Call<PageLead> call, @NonNull Response<PageLead> rsp) {
                        app.getSessionFunnel().leadSectionFetchEnd();
                        PageLead lead = rsp.body();
                        pageLoadLeadSectionComplete(lead, startSequenceNum);
                        if (rsp.raw().cacheResponse() != null) {
                            showPageOfflineMessage(rsp.raw().header("date", ""));
                        }
                    }

                    @Override public void onFailure(@NonNull Call<PageLead> call, @NonNull Throwable t) {
                        if (OfflineManager.instance().titleExists(model.getTitle().getDisplayText())) {
                            pageLoadFromCompilation();
                            return;
                        }
                        L.e("PageLead error: ", t);
                        commonSectionFetchOnCatch(t, startSequenceNum);
                    }
                });
    }

    private void pageLoadFromCompilation() {
        String normalizedTitle = OfflineManager.instance().getNormalizedTitle(model.getTitle().getDisplayText());
        PageTitle newTitle = TextUtils.isEmpty(normalizedTitle) ? model.getTitle()
                : new PageTitle(normalizedTitle, model.getTitle().getWikiSite());

        Page page = new Page(newTitle, new ArrayList<>(),
                new PageProperties(newTitle, OfflineManager.instance().isMainPage(newTitle.getDisplayText())));

        model.setPage(page);
        editHandler.setPage(model.getPage());

        leadImagesHandler.beginLayout((sequence) -> {
            if (!fragment.isAdded() || !sequenceNumber.inSync(sequence)) {
                return;
            }
            fragment.setToolbarFadeEnabled(leadImagesHandler.isLeadImageEnabled());
            loadContentsFromCompilation();
        }, sequenceNumber.get());

        if (webView.getVisibility() != View.VISIBLE) {
            webView.setVisibility(View.VISIBLE);
        }

        refreshView.setRefreshing(false);
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
    }

    private void loadContentsFromCompilation() {
        try {
            Page page = model.getPage();
            sendMarginPayload();
            OfflineManager.HtmlResult result = OfflineManager.instance()
                    .getHtmlForTitle(model.getTitle().getDisplayText());
            page.setCompilation(result.compilation());
            JSONObject zimPayload = setLeadSectionMetadata(new JSONObject(), page)
                    .put("zimhtml", result.html())
                    .put("fromRestBase", false)
                    .put("offlineContentProvider", OfflineContentProvider.getBaseUrl());
            if (page.isMainPage()) {
                zimPayload.put("mainPageHint", fragment.getString(R.string.offline_library_main_page_hint_html));
            }

            if (sectionTargetFromTitle != null) {
                //if we have a section to scroll to (from our PageTitle):
                zimPayload.put("fragment", sectionTargetFromTitle);
            } else if (!TextUtils.isEmpty(model.getTitle().getFragment())) {
                // It's possible that the link was a redirect and the new title has a fragment
                // scroll to it, if there was no fragment so far
                zimPayload.put("fragment", model.getTitle().getFragment());
            }

            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            zimPayload.put("scrollY", (int) (stagedScrollY / DimenUtil.getDensityScalar()));
            bridge.sendMessage("displayFromZim", zimPayload);
            showOfflineCompilationMessage(result.compilation().name(), result.compilation().date());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            fragment.onPageLoadError(e);
        }
        loading = false;
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

    private void pageLoadDisplayLeadSection() {
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

    private JSONObject setLeadSectionMetadata(@NonNull JSONObject obj,
                                              @NonNull Page page) throws JSONException {
        SparseArray<String> localizedStrings = localizedStrings(page);
        return obj.put("sequence", sequenceNumber.get())
                .put("title", page.getDisplayTitle())
                .put("string_table_infobox", localizedStrings.get(R.string.table_infobox))
                .put("string_table_other", localizedStrings.get(R.string.table_other))
                .put("string_table_close", localizedStrings.get(R.string.table_close))
                .put("string_expand_refs", localizedStrings.get(R.string.expand_refs))
                .put("isBeta", ReleaseUtil.isPreProdRelease()) // True for any non-production release type
                .put("siteLanguage", model.getTitle().getWikiSite().languageCode())
                .put("siteBaseUrl", model.getTitle().getWikiSite().url())
                .put("isMainPage", page.isMainPage())
                .put("isFilePage", page.isFilePage())
                .put("fromRestBase", page.isFromRestBase())
                .put("apiLevel", Build.VERSION.SDK_INT)
                .put("showImages", Prefs.isImageDownloadEnabled());
    }

    private JSONObject leadSectionPayload(@NonNull Page page) {

        try {
            return setLeadSectionMetadata(new JSONObject(), page)
                    .put("section", page.getSections().get(0).toJSON());
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
        return (AccountUtil.isLoggedIn() || !isAnonEditingDisabled())
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

    private void showOfflineCompilationMessage(@NonNull String compName, @NonNull Date date) {
        if (fragment.isAdded()) {
            String dateStr = DateUtil.getShortDateString(date);
            Toast.makeText(fragment.getContext().getApplicationContext(),
                    fragment.getString(R.string.page_offline_notice_compilation_download_date, compName, dateStr),
                    Toast.LENGTH_LONG).show();
        }
    }

    private JSONObject getRemoteConfig() {
        return app.getRemoteConfig().getConfig();
    }

    private void queueRemainingSections(@NonNull Request request) {
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
        try {
            JSONObject wrapper = new JSONObject();
            wrapper.put("sequence", sequenceNumber.get());
            wrapper.put("url", request.url());

            if (sectionTargetFromIntent > 0 && sectionTargetFromIntent < model.getPage().getSections().size()) {
                //if we have a section to scroll to (from our Intent):
                wrapper.put("fragment", model.getPage().getSections().get(sectionTargetFromIntent).getAnchor());
            } else if (sectionTargetFromTitle != null) {
                //if we have a section to scroll to (from our PageTitle):
                wrapper.put("fragment", sectionTargetFromTitle);
            } else if (!TextUtils.isEmpty(model.getTitle().getFragment())) {
                // It's possible, that the link was a redirect and the new title has a fragment
                // scroll to it, if there was no fragment so far
                wrapper.put("fragment", model.getTitle().getFragment());
            }

            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY", (int) (stagedScrollY / DimenUtil.getDensityScalar()));
            bridge.sendMessage("queueRemainingSections", wrapper);
        } catch (JSONException e) {
            L.logRemoteErrorIfProd(e);
        }
    }

    private void pageLoadLeadSectionComplete(PageLead pageLead, int startSequenceNum) {
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        if (pageLead.hasError()) {
            ServiceError error = pageLead.getError();
            if (error != null) {
                commonSectionFetchOnCatch(new MwException((MwServiceError) error), startSequenceNum);
            } else {
                commonSectionFetchOnCatch(new IOException(getResources().getString(R.string.error_unknown)), startSequenceNum);
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

        layoutLeadImage(() -> {
            if (!fragment.isAdded()) {
                return;
            }
            fragment.callback().onPageInvalidateOptionsMenu();
            pageLoadRemainingSections(sequenceNumber.get());
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

    private void pageLoadRemainingSections(final int startSequenceNum) {
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        app.getSessionFunnel().restSectionsFetchStart();

        Request request = PageClientFactory
                .create(model.getTitle().getWikiSite(), model.getTitle().namespace())
                .sections(model.shouldForceNetwork() ? CacheControl.FORCE_NETWORK : null,
                        model.shouldSaveOffline() ? PageClient.CacheOption.SAVE : PageClient.CacheOption.CACHE,
                        model.getTitle().getPrefixedText())
                .request();

        queueRemainingSections(request);
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
            fragment.setToolbarFadeEnabled(leadImagesHandler.isLeadImageEnabled());

            if (runnable != null) {
                // when the lead image is laid out, load the lead section and the rest
                // of the sections into the webview.
                pageLoadDisplayLeadSection();
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
