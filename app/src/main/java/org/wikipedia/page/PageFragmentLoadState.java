package org.wikipedia.page;

import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.Response;

/**
 * Our  page load strategy, which uses responses from the following to construct the page:
 * page/summary end-point.
 * page/media-list end-point.
 * Data received from the javaScript interface
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

    @NonNull private Tab currentTab = new Tab();

    private ErrorCallback networkErrorCallback;

    // copied fields
    private PageViewModel model;
    private PageFragment fragment;
    private CommunicationBridge bridge;
    private ObservableWebView webView;
    private WikipediaApp app = WikipediaApp.getInstance();
    private LeadImagesHandler leadImagesHandler;
    private CompositeDisposable disposables = new CompositeDisposable();

    @SuppressWarnings("checkstyle:parameternumber")
    public void setUp(@NonNull PageViewModel model,
                      @NonNull PageFragment fragment,
                      @NonNull ObservableWebView webView,
                      @NonNull CommunicationBridge bridge,
                      @NonNull LeadImagesHandler leadImagesHandler,
                      @NonNull Tab tab) {
        this.model = model;
        this.fragment = fragment;
        this.webView = webView;
        this.bridge = bridge;
        this.leadImagesHandler = leadImagesHandler;

        this.currentTab = tab;
    }

    public void load(boolean pushBackStack) {
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateCurrentBackStackItem();
            currentTab.pushBackStackItem(new PageBackStackItem(model.getTitle(), model.getCurEntry()));
        }
        pageLoadCheckReadingLists();
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

    public void onConfigurationChanged() {
        leadImagesHandler.loadLeadImage();
        bridge.execute(JavaScriptActionHandler.setTopMargin(leadImagesHandler.getTopMargin()));
    }

    protected void commonSectionFetchOnCatch(@NonNull Throwable caught) {
        if (!fragment.isAdded()) {
            return;
        }
        ErrorCallback callback = networkErrorCallback;
        networkErrorCallback = null;
        fragment.requireActivity().invalidateOptionsMenu();
        if (callback != null) {
            callback.call(caught);
        }
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

        networkErrorCallback = errorCallback;
        if (!fragment.isAdded()) {
            return;
        }
        fragment.requireActivity().invalidateOptionsMenu();
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true);
        }

        app.getSessionFunnel().leadSectionFetchStart();

        model.setPage(null);
        boolean delayLoadHtml = model.getTitle().getPrefixedText().contains(":");

        if (!delayLoadHtml) {
            bridge.resetHtml(model.getTitle());
        }

        if (model.getTitle().namespace() == Namespace.SPECIAL) {
            // Short-circuit the entire process of fetching the Summary, since Special: pages
            // are not supported in RestBase.
            bridge.resetHtml(model.getTitle());
            leadImagesHandler.loadLeadImage();
            fragment.requireActivity().invalidateOptionsMenu();
            fragment.onPageMetadataLoaded();
            return;
        }

        disposables.add(Observable.zip(ServiceFactory.getRest(model.getTitle().getWikiSite())
                .getSummaryResponse(model.getTitle().getPrefixedText(), null, model.getCacheControl().toString(),
                        model.shouldSaveOffline() ? OfflineCacheInterceptor.SAVE_HEADER_SAVE : null,
                        model.getTitle().getWikiSite().languageCode(), UriUtil.encodeURL(model.getTitle().getPrefixedText())),
                (app.isOnline() && AccountUtil.isLoggedIn()) ? ServiceFactory.get(model.getTitle().getWikiSite()).getWatchedInfo(model.getTitle().getPrefixedText()) : Observable.just(new MwQueryResponse()), Pair::new)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> {
                    Response<PageSummary> pageSummaryResponse = pair.first;
                    MwQueryResponse watchedResponse = pair.second;

                    boolean isWatched = false;
                    boolean hasWatchlistExpiry = false;
                    if (watchedResponse != null && watchedResponse.query() != null && watchedResponse.query().firstPage() != null) {
                        isWatched = watchedResponse.query().firstPage().isWatched();
                        hasWatchlistExpiry = watchedResponse.query().firstPage().hasWatchlistExpiry();
                    }

                    if (pageSummaryResponse.body() == null) {
                        throw new RuntimeException("Summary response was invalid.");
                    }

                    createPageModel(pageSummaryResponse, isWatched, hasWatchlistExpiry);

                    if (OfflineCacheInterceptor.SAVE_HEADER_SAVE.equals(pageSummaryResponse.headers().get(OfflineCacheInterceptor.SAVE_HEADER))) {
                        showPageOfflineMessage(pageSummaryResponse.raw().header("date", ""));
                    }

                    if (delayLoadHtml) {
                        bridge.resetHtml(model.getTitle());
                    }

                    fragment.onPageMetadataLoaded();
                }, throwable -> {
                    L.e("Page details network response error: ", throwable);
                    commonSectionFetchOnCatch(throwable);
                })
        );
    }

    private void showPageOfflineMessage(@Nullable String dateHeader) {
        if (!fragment.isAdded()) {
            return;
        }
        try {
            String dateStr = DateUtil.getShortDateString(DateUtil
                    .getHttpLastModifiedDate(dateHeader));
            Toast.makeText(fragment.requireContext().getApplicationContext(),
                    fragment.getString(R.string.page_offline_notice_last_date, dateStr),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // ignore
        }
    }

    private void createPageModel(@NonNull Response<PageSummary> response,
                                 boolean isWatched,
                                 boolean hasWatchlistExpiry) {
        if (!fragment.isAdded() || response.body() == null) {
            return;
        }
        PageSummary pageSummary = response.body();
        Page page = pageSummary.toPage(model.getTitle());

        model.setPage(page);
        model.setWatched(isWatched);
        model.hasWatchlistExpiry(hasWatchlistExpiry);
        model.setTitle(page.getTitle());

        if (!TextUtils.isEmpty(response.raw().request().url().fragment())) {
            model.getTitle().setFragment(response.raw().request().url().fragment());
        }

        if (page.getTitle().getDescription() == null) {
            app.getSessionFunnel().noDescription();
        }

        if (!model.getTitle().isMainPage()) {
            model.getTitle().setDisplayText(page.getDisplayTitle());
        }

        leadImagesHandler.loadLeadImage();

        fragment.requireActivity().invalidateOptionsMenu();

        // Update our history entry, in case the Title was changed (i.e. normalized)
        final HistoryEntry curEntry = model.getCurEntry();
        model.setCurEntry(new HistoryEntry(model.getTitle(), curEntry.getTimestamp(), curEntry.getSource()));
        model.getCurEntry().setReferrer(curEntry.getReferrer());

        // Update our tab list to prevent ZH variants issue.
        if (app.getTabList().get(app.getTabCount() - 1) != null) {
            app.getTabList().get(app.getTabCount() - 1).setBackStackPositionTitle(model.getTitle());
        }

        // Save the thumbnail URL to the DB
        PageImage pageImage = new PageImage(model.getTitle(), pageSummary.getThumbnailUrl());
        Completable.fromAction(() -> app.getDatabaseClient(PageImage.class).upsert(pageImage, PageImageHistoryContract.Image.SELECTION)).subscribeOn(Schedulers.io()).subscribe();

        model.getTitle().setThumbUrl(pageImage.getImageName());
    }
}
