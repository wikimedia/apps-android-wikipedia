package org.wikipedia.page;

import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.descriptions.DescriptionEditUtil;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.edit.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.CacheControl;
import okhttp3.Protocol;
import okhttp3.Request;

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
        void call(@NonNull Throwable error);
    }

    private boolean loading;

    @NonNull private Tab currentTab = new Tab();

    @NonNull private final SequenceNumber sequenceNumber = new SequenceNumber();

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
    private CompositeDisposable disposables = new CompositeDisposable();

    @SuppressWarnings("checkstyle:parameternumber")
    public void setUp(@NonNull PageViewModel model,
                      @NonNull PageFragment fragment,
                      @NonNull SwipeRefreshLayoutWithScroll refreshView,
                      @NonNull ObservableWebView webView,
                      @NonNull CommunicationBridge bridge,
                      @NonNull LeadImagesHandler leadImagesHandler,
                      @NonNull Tab tab) {
        this.model = model;
        this.fragment = fragment;
        this.refreshView = refreshView;
        this.webView = webView;
        this.bridge = bridge;
        this.leadImagesHandler = leadImagesHandler;

        setUpBridgeListeners();

        this.currentTab = tab;
    }

    public void load(boolean pushBackStack) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem();
            currentTab.pushBackStackItem(new PageBackStackItem(model.getTitleOriginal(), model.getCurEntry()));
        }

        loading = true;

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        sequenceNumber.increase();

        pageLoadCheckReadingLists();
    }

    public boolean isLoading() {
        return loading;
    }

    public void loadFromBackStack() {
        if (currentTab.getBackStack().isEmpty()) {
            return;
        }
        PageBackStackItem item = currentTab.getBackStack().get(currentTab.getBackStackPosition());
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        fragment.loadPage(item.getTitle(), item.getHistoryEntry(), false, item.getScrollY());
        L.d("Loaded page " + item.getTitle().getDisplayText() + " from backstack");
    }

    public void updateCurrentBackStackItem() {
        if (currentTab.getBackStack().isEmpty()) {
            return;
        }
        PageBackStackItem item = currentTab.getBackStack().get(currentTab.getBackStackPosition());
        item.setScrollY(webView.getScrollY());
        if (model.getTitle() != null) {
            // Preserve metadata of the current PageTitle into our backstack, so that
            // this data would be available immediately upon loading PageFragment, instead
            // of only after loading the lead section.
            item.getTitle().setDescription(model.getTitle().getDescription());
            item.getTitle().setThumbUrl(model.getTitle().getThumbUrl());
        }
    }

    public void setTab(@NonNull Tab tab) {
        this.currentTab = tab;
    }

    public boolean goBack() {
        if (currentTab.canGoBack()) {
            currentTab.moveBack();
            if (!backStackEmpty()) {
                loadFromBackStack();
                return true;
            }
        }
        return false;
    }

    public boolean goForward() {
        if (currentTab.canGoForward()) {
            currentTab.moveForward();
            loadFromBackStack();
            return true;
        }
        return false;
    }

    public boolean backStackEmpty() {
        return currentTab.getBackStack().isEmpty();
    }

    public void setEditHandler(EditHandler editHandler) {
        this.editHandler = editHandler;
    }

    public void backFromEditing(Intent data) {
        //Retrieve section ID from intent, and find correct section, so where know where to scroll to
        sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
    }

    public void onConfigurationChanged() {
        leadImagesHandler.loadLeadImage();
        leadImagesHandler.setWebViewPaddingTop();
        fragment.setToolbarFadeEnabled(leadImagesHandler.isLeadImageEnabled());
    }

    public boolean isFirstPage() {
        return currentTab.getBackStack().size() <= 1 && !webView.canGoBack();
    }

    private void commonSectionFetchOnCatch(@NonNull Throwable caught, int startSequenceNum) {
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }
        ErrorCallback callback = networkErrorCallback;
        networkErrorCallback = null;
        loading = false;
        fragment.requireActivity().invalidateOptionsMenu();
        if (callback != null) {
            callback.call(caught);
        }
    }

    private void setUpBridgeListeners() {
        bridge.addListener("loadRemainingError", (messageType, payload) -> {
            try {
                if (!fragment.isAdded() || model.getTitle() == null || !sequenceNumber.inSync(payload.getInt("sequence"))) {
                    return;
                }
                commonSectionFetchOnCatch(new HttpStatusException(new okhttp3.Response.Builder()
                        .code(payload.getInt("status")).protocol(Protocol.HTTP_1_1).message("")
                        .request(new Request.Builder()
                                .url(model.getTitle().getMobileUri()).build()).build()),
                        payload.getInt("sequence"));
            } catch (JSONException e) {
                L.logRemoteErrorIfProd(e);
            }
        });
        bridge.addListener("pageLoadComplete", (messageType, payload) -> {
            if (!fragment.isAdded()) {
                return;
            }
            app.getSessionFunnel().restSectionsFetchEnd();
            if (fragment.callback() != null) {
                fragment.callback().onPageUpdateProgressBar(false, true, 0);
            }
            try {
                if (model.getPage() == null || !sequenceNumber.inSync(payload.getInt("sequence"))) {
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
        });
    }

    private void pageLoadCheckReadingLists() {
        disposables.clear();
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().findPageInAnyList(model.getTitle()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> pageLoadFromNetwork((final Throwable networkError) -> fragment.onPageLoadError(networkError)))
                .subscribe(page -> model.setReadingListPage(page),
                        throwable -> model.setReadingListPage(null)));
    }

    private void pageLoadFromNetwork(final ErrorCallback errorCallback) {
        if (model.getTitle() == null) {
            return;
        }
        fragment.updateBookmarkAndMenuOptions();
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = model.getTitle().getFragment();

        L10nUtil.setupDirectionality(model.getTitle().getWikiSite().languageCode(), Locale.getDefault(),
                bridge);

        networkErrorCallback = errorCallback;
        if (!fragment.isAdded()) {
            return;
        }
        loading = true;
        fragment.requireActivity().invalidateOptionsMenu();
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
        pageLoadLeadSection(sequenceNumber.get());
    }

    private void pageLoadLeadSection(final int startSequenceNum) {
        app.getSessionFunnel().leadSectionFetchStart();

        disposables.add(PageClientFactory.create(model.getTitle().getWikiSite(), model.getTitle().namespace())
                .lead(model.getTitle().getWikiSite(), model.getCacheControl(), model.shouldSaveOffline() ? OfflineCacheInterceptor.SAVE_HEADER_SAVE : null,
                        model.getCurEntry().getReferrer(), model.getTitle().getConvertedText(), calculateLeadImageWidth())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(rsp -> {
                    app.getSessionFunnel().leadSectionFetchEnd();
                    PageLead lead = rsp.body();
                    pageLoadLeadSectionComplete(lead, startSequenceNum);
                    if ((rsp.raw().cacheResponse() != null && rsp.raw().networkResponse() == null)
                            || OfflineCacheInterceptor.SAVE_HEADER_SAVE.equals(rsp.headers().get(OfflineCacheInterceptor.SAVE_HEADER))) {
                        showPageOfflineMessage(rsp.raw().header("date", ""));
                    }
                }, t -> {
                    L.e("PageLead error: ", t);
                    commonSectionFetchOnCatch(t, startSequenceNum);
                }));
    }

    private void pageLoadDisplayLeadSection() {
        Page page = model.getPage();

        Request remainingRequest = PageClientFactory.create(model.getTitle().getWikiSite(), model.getTitle().namespace())
                .sectionsUrl(model.getTitle().getWikiSite(), model.shouldForceNetwork() ? CacheControl.FORCE_NETWORK : null,
                        model.shouldSaveOffline() ? OfflineCacheInterceptor.SAVE_HEADER_SAVE : null,
                        model.getTitle().getConvertedText());

        sendLeadSectionPayload(page, remainingRequest.url().toString());

        if (webView.getVisibility() != View.VISIBLE) {
            webView.setVisibility(View.VISIBLE);
        }

        refreshView.setRefreshing(false);
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }
    }

    private void sendLeadSectionPayload(@NonNull Page page, @NonNull String remainingUrl) {
        JSONObject leadSectionPayload = leadSectionPayload(page, remainingUrl);
        bridge.sendMessage("displayLeadSection", leadSectionPayload);
        app.getSessionFunnel().restSectionsFetchStart();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private JSONObject setLeadSectionMetadata(@NonNull JSONObject obj,
                                              @NonNull Page page) throws JSONException {
        SparseArray<String> localizedStrings = localizedStrings(page);
        return obj.put("sequence", sequenceNumber.get())
                .put("title", page.getDisplayTitle())
                .put("description", StringUtils.capitalize(model.getTitle().getDescription()))
                .put("allowDescriptionEdit", DescriptionEditUtil.isEditAllowed(page))
                .put("hasPronunciation", !TextUtils.isEmpty(page.getTitlePronunciationUrl()))
                .put("string_table_infobox", localizedStrings.get(R.string.table_infobox))
                .put("string_table_other", localizedStrings.get(R.string.table_other))
                .put("string_table_close", localizedStrings.get(R.string.table_close))
                .put("string_expand_refs", localizedStrings.get(R.string.expand_refs))
                .put("string_add_description", localizedStrings.get(R.string.description_edit_add_description))
                .put("isBeta", ReleaseUtil.isPreProdRelease()) // True for any non-production release type
                .put("siteLanguage", model.getTitle().getWikiSite().languageCode())
                .put("siteBaseUrl", model.getTitle().getWikiSite().url())
                .put("isMainPage", page.isMainPage())
                .put("isFilePage", page.isFilePage())
                .put("fromRestBase", page.isFromRestBase())
                .put("apiLevel", Build.VERSION.SDK_INT)
                .put("showImages", Prefs.isImageDownloadEnabled())
                .put("collapseTables", Prefs.isCollapseTablesEnabled())
                .put("theme", app.getCurrentTheme().getPageLibClass())
                .put("imagePlaceholderBackgroundColor", "#" + Integer.toHexString(ResourceUtil.getThemedColor(fragment.requireContext(), android.R.attr.colorBackground) & 0xFFFFFF))
                .put("dimImages", app.getCurrentTheme().isDark() && Prefs.shouldDimDarkModeImages())
                .put("paddingTop", leadImagesHandler.getPaddingTop())
                .put("noedit", !isPageEditable(page)) // Controls whether edit pencils are visible.
                .put("protect", page.isProtected());
    }

    private JSONObject leadSectionPayload(@NonNull Page page, @NonNull String remainingUrl) {
        try {
            JSONObject wrapper = setLeadSectionMetadata(new JSONObject(), page)
                    .put("section", page.getSections().get(0).toJSON())
                    .put("remainingUrl", remainingUrl);

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

            return wrapper;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private SparseArray<String> localizedStrings(Page page) {
        return getStringsForArticleLanguage(page.getTitle(),
                ResourceUtil.getIdArray(fragment.requireContext(), R.array.page_localized_string_ids));
    }

    private boolean isPageEditable(Page page) {
        return (AccountUtil.isLoggedIn() || !isAnonEditingDisabled())
                && !page.isFilePage()
                && !page.isMainPage();
    }

    private boolean isAnonEditingDisabled() {
        return app.getRemoteConfig().getConfig().optBoolean("disableAnonEditing", false);
    }

    private void showPageOfflineMessage(@NonNull String dateHeader) {
        if (!fragment.isAdded()) {
            return;
        }
        try {
            String dateStr = DateUtil.getShortDateString(DateUtil
                    .getHttpLastModifiedDate(dateHeader));
            Toast.makeText(fragment.requireContext().getApplicationContext(),
                    fragment.getString(R.string.page_offline_notice_last_date, dateStr),
                    Toast.LENGTH_LONG).show();
        } catch (ParseException e) {
            // ignore
        }
    }

    private void pageLoadLeadSectionComplete(PageLead pageLead, int startSequenceNum) {
        if (!fragment.isAdded() || !sequenceNumber.inSync(startSequenceNum)) {
            return;
        }

        Page page = pageLead.toPage(model.getTitle());
        model.setPage(page);
        model.setTitle(page.getTitle());

        editHandler.setPage(model.getPage());

        if (page.getTitle().getDescription() == null) {
            app.getSessionFunnel().noDescription();
        }

        leadImagesHandler.loadLeadImage();

        fragment.setToolbarFadeEnabled(leadImagesHandler.isLeadImageEnabled());
        fragment.requireActivity().invalidateOptionsMenu();

        // Update our history entry, in case the Title was changed (i.e. normalized)
        final HistoryEntry curEntry = model.getCurEntry();
        model.setCurEntry(new HistoryEntry(model.getTitle(), curEntry.getTimestamp(), curEntry.getSource()));
        model.getCurEntry().setReferrer(curEntry.getReferrer());

        // Save the thumbnail URL to the DB
        PageImage pageImage = new PageImage(model.getTitle(), pageLead.getThumbUrl());
        Completable.fromAction(() -> app.getDatabaseClient(PageImage.class).upsert(pageImage, PageImageHistoryContract.Image.SELECTION)).subscribeOn(Schedulers.io()).subscribe();

        model.getTitle().setThumbUrl(pageImage.getImageName());
        model.getTitleOriginal().setThumbUrl(pageImage.getImageName());

        pageLoadDisplayLeadSection();
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
