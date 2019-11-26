package org.wikipedia.page;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.database.contract.PageImageHistoryContract;
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.edit.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;

import java.text.ParseException;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

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
    private WikipediaApp app = WikipediaApp.getInstance();
    private LeadImagesHandler leadImagesHandler;
    private EditHandler editHandler;
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

    public void onPageFinished() {
        bridge.onPageFinished();
        loading = false;
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
        bridge.execute(JavaScriptActionHandler.setTopMargin(leadImagesHandler.getPaddingTop()));
        fragment.setToolbarFadeEnabled(leadImagesHandler.isLeadImageEnabled());
    }

    public boolean isFirstPage() {
        return currentTab.getBackStack().size() <= 1 && !webView.canGoBack();
    }

    protected void commonSectionFetchOnCatch(@NonNull Throwable caught) {
        if (!fragment.isAdded()) {
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

        networkErrorCallback = errorCallback;
        if (!fragment.isAdded()) {
            return;
        }
        loading = true;
        fragment.requireActivity().invalidateOptionsMenu();
        if (fragment.callback() != null) {
            fragment.callback().onPageUpdateProgressBar(true, true, 0);
        }

        app.getSessionFunnel().leadSectionFetchStart();

        ///Make a call to PageSummary
        //update page with protection and sections

        disposables.add(new PageClient().summary(model.getTitle().getWikiSite(), model.getTitle().getPrefixedText(), null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribe(response -> {
                    //app.getSessionFunnel().leadSectionFetchEnd();
                    pageLoadLeadSectionComplete(response.body());

                    bridge.execute(JavaScriptActionHandler.setFooter(fragment.requireContext(), model));

                    if ((response.raw().cacheResponse() != null && response.raw().networkResponse() == null)
                            || OfflineCacheInterceptor.SAVE_HEADER_SAVE.equals(response.headers().get(OfflineCacheInterceptor.SAVE_HEADER))) {
                        showPageOfflineMessage(response.raw().header("date", ""));
                    }
                }, t -> {
                    L.e("PageLead error: ", t);
                    commonSectionFetchOnCatch(t);
                }));

        // And finally, start blasting the HTML into the WebView.
        bridge.resetHtml(model.getTitle().getWikiSite().url(), model.getTitle().getPrefixedText());
    }

    private void updateThumbnail(String thumbUrl) {
        model.getTitle().setThumbUrl(thumbUrl);
        model.getTitleOriginal().setThumbUrl(thumbUrl);
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

    private void pageLoadLeadSectionComplete(PageSummary pageSummary) {
        if (!fragment.isAdded()) {
            return;
        }

        Page page = pageSummary.toPage(model.getTitle(), null);
        bridge.execute(JavaScriptActionHandler.setUpEditButtons(true, !page.getPageProperties().canEdit()));

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
        PageImage pageImage = new PageImage(model.getTitle(), pageSummary.getThumbnailUrl());
        Completable.fromAction(() -> app.getDatabaseClient(PageImage.class).upsert(pageImage, PageImageHistoryContract.Image.SELECTION)).subscribeOn(Schedulers.io()).subscribe();

        model.getTitle().setThumbUrl(pageImage.getImageName());
        model.getTitleOriginal().setThumbUrl(pageImage.getImageName());
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
