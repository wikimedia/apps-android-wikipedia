package org.wikipedia.page;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.FindInPageFunnel;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.analytics.PageScrollFunnel;
import org.wikipedia.analytics.TabFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.descriptions.DescriptionEditTutorialActivity;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.UpdateHistoryTask;
import org.wikipedia.language.LangLinksActivity;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.media.AvPlayer;
import org.wikipedia.media.DefaultAvPlayer;
import org.wikipedia.media.MediaPlayerImplementation;
import org.wikipedia.page.action.PageActionTab;
import org.wikipedia.page.action.PageActionToolbarHideHandler;
import org.wikipedia.page.bottomcontent.BottomContentView;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.leadimages.PageHeaderView;
import org.wikipedia.page.shareafact.ShareHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.ActiveTimer;
import org.wikipedia.util.AnimationUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;
import org.wikipedia.views.WikiPageErrorView;

import java.util.Date;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.page.PageActivity.ACTION_RESUME_READING;
import static org.wikipedia.page.PageCacher.loadIntoCache;
import static org.wikipedia.settings.Prefs.isDescriptionEditTutorialEnabled;
import static org.wikipedia.settings.Prefs.isLinkPreviewEnabled;
import static org.wikipedia.util.DimenUtil.getContentTopOffset;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.DimenUtil.leadImageHeightForDevice;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.util.ResourceUtil.getThemedColor;
import static org.wikipedia.util.StringUtil.addUnderscores;
import static org.wikipedia.util.ThrowableUtil.isOffline;
import static org.wikipedia.util.UriUtil.decodeURL;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class PageFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onPageShowBottomSheet(@NonNull BottomSheetDialog dialog);
        void onPageShowBottomSheet(@NonNull BottomSheetDialogFragment dialog);
        void onPageDismissBottomSheet();
        void onPageLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry);
        void onPageInitWebView(@NonNull ObservableWebView v);
        void onPageShowLinkPreview(@NonNull HistoryEntry entry);
        void onPageLoadMainPageInForegroundTab();
        void onPageUpdateProgressBar(boolean visible, boolean indeterminate, int value);
        void onPageShowThemeChooser();
        void onPageStartSupportActionMode(@NonNull ActionMode.Callback callback);
        void onPageShowToolbar();
        void onPageHideSoftKeyboard();
        void onPageAddToReadingList(@NonNull PageTitle title,
                                    @NonNull AddToReadingListDialog.InvokeSource source);
        void onPageRemoveFromReadingLists(@NonNull PageTitle title);
        void onPageLoadError(@NonNull PageTitle title);
        void onPageLoadErrorBackPressed();
        void onPageHideAllContent();
        void onPageSetToolbarFadeEnabled(boolean enabled);
        void onPageSetToolbarElevationEnabled(boolean enabled);
    }

    private boolean pageRefreshed;
    private boolean errorState = false;

    private static final int REFRESH_SPINNER_ADDITIONAL_OFFSET = (int) (16 * DimenUtil.getDensityScalar());

    private PageFragmentLoadState pageFragmentLoadState;
    private PageViewModel model;

    @NonNull private TabFunnel tabFunnel = new TabFunnel();

    private PageScrollFunnel pageScrollFunnel;
    private LeadImagesHandler leadImagesHandler;
    private PageHeaderView pageHeaderView;
    private BottomContentView bottomContentView;
    private ObservableWebView webView;
    private CoordinatorLayout containerView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikiPageErrorView errorView;
    private PageActionTabLayout tabLayout;
    private ToCHandler tocHandler;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private EditHandler editHandler;
    private ActionMode findInPageActionMode;
    private ShareHandler shareHandler;
    private CompositeDisposable disposables = new CompositeDisposable();
    private ActiveTimer activeTimer = new ActiveTimer();
    @Nullable private AvPlayer avPlayer;
    @Nullable private AvCallback avCallback;

    private WikipediaApp app;

    @NonNull
    private final SwipeRefreshLayout.OnRefreshListener pageRefreshListener = this::refreshPage;

    private PageActionTab.Callback pageActionTabsCallback = new PageActionTab.Callback() {
        @Override
        public void onAddToReadingListTabSelected() {
            Prefs.shouldShowBookmarkToolTip(false);
            if (model.isInReadingList()) {
                new ReadingListBookmarkMenu(tabLayout, new ReadingListBookmarkMenu.Callback() {
                    @Override
                    public void onAddRequest(@Nullable ReadingListPage page) {
                        addToReadingList(getTitle(), AddToReadingListDialog.InvokeSource.BOOKMARK_BUTTON);
                    }

                    @Override
                    public void onDeleted(@Nullable ReadingListPage page) {
                        if (callback() != null) {
                            callback().onPageRemoveFromReadingLists(getTitle());
                        }
                    }

                    @Override
                    public void onShare() {
                        // ignore
                    }
                }).show(getTitle());
            } else {
                addToReadingList(getTitle(), AddToReadingListDialog.InvokeSource.BOOKMARK_BUTTON);
            }
        }

        @Override
        public void onSharePageTabSelected() {
            sharePageLink();
        }

        @Override
        public void onChooseLangTabSelected() {
            startLangLinksActivity();
        }

        @Override
        public void onFindInPageTabSelected() {
            showFindInPage();
        }

        @Override
        public void onFontAndThemeTabSelected() {
            showThemeChooser();
        }

        @Override
        public void onViewToCTabSelected() {
            tocHandler.show();
        }

        @Override
        public void updateBookmark(boolean pageSaved) {
            setBookmarkIconForPageSavedState(pageSaved);
        }
    };

    public ObservableWebView getWebView() {
        return webView;
    }

    public PageTitle getTitle() {
        return model.getTitle();
    }

    @Nullable public PageTitle getTitleOriginal() {
        return model.getTitleOriginal();
    }

    @NonNull public ShareHandler getShareHandler() {
        return shareHandler;
    }

    @Nullable public Page getPage() {
        return model.getPage();
    }

    public HistoryEntry getHistoryEntry() {
        return model.getCurEntry();
    }

    public EditHandler getEditHandler() {
        return editHandler;
    }

    public BottomContentView getBottomContentView() {
        return bottomContentView;
    }

    public ViewGroup getContainerView() {
        return containerView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnimationUtil.setSharedElementTransitions(requireActivity());
        app = (WikipediaApp) requireActivity().getApplicationContext();
        model = new PageViewModel();
        pageFragmentLoadState = new PageFragmentLoadState();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);
        pageHeaderView = rootView.findViewById(R.id.page_header_view);
        DimenUtil.setViewHeight(pageHeaderView, leadImageHeightForDevice());

        webView = rootView.findViewById(R.id.page_web_view);
        initWebViewListeners();

        containerView = rootView.findViewById(R.id.page_contents_container);
        refreshView = rootView.findViewById(R.id.page_refresh_container);
        int swipeOffset = getContentTopOffsetPx(requireActivity()) + REFRESH_SPINNER_ADDITIONAL_OFFSET;
        refreshView.setProgressViewOffset(false, -swipeOffset, swipeOffset);
        refreshView.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        refreshView.setScrollableChild(webView);
        refreshView.setOnRefreshListener(pageRefreshListener);

        tabLayout = rootView.findViewById(R.id.page_actions_tab_layout);
        tabLayout.setPageActionTabsCallback(pageActionTabsCallback);

        errorView = rootView.findViewById(R.id.page_error);

        bottomContentView = rootView.findViewById(R.id.page_bottom_view);

        PageActionToolbarHideHandler pageActionToolbarHideHandler
                = new PageActionToolbarHideHandler(tabLayout, null);
        pageActionToolbarHideHandler.setScrollView(webView);

        PageActionToolbarHideHandler snackbarHideHandler =
                new PageActionToolbarHideHandler(rootView.findViewById(R.id.fragment_page_coordinator), null);
        snackbarHideHandler.setScrollView(webView);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        if (avPlayer != null) {
            avPlayer.deinit();
            avPlayer = null;
        }
        //uninitialize the bridge, so that no further JS events can have any effect.
        bridge.cleanup();
        shareHandler.dispose();
        bottomContentView.dispose();
        disposables.clear();
        webView.clearAllListeners();
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.getRefWatcher().watch(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        if (callback() != null) {
            callback().onPageInitWebView(webView);
        }

        updateFontSize();

        // Explicitly set background color of the WebView (independently of CSS, because
        // the background may be shown momentarily while the WebView loads content,
        // creating a seizure-inducing effect, or at the very least, a migraine with aura).
        webView.setBackgroundColor(getThemedColor(requireActivity(), R.attr.paper_color));

        bridge = new CommunicationBridge(webView);
        setupMessageHandlers();
        sendDecorOffsetMessage();

        errorView.setRetryClickListener((v) -> refreshPage());
        errorView.setBackClickListener((v) -> {
            boolean back = onBackPressed();

            // Needed if we're coming from another activity or fragment
            if (!back && callback() != null) {
                // noinspection ConstantConditions
                callback().onPageLoadErrorBackPressed();
            }
        });

        editHandler = new EditHandler(this, bridge);
        pageFragmentLoadState.setEditHandler(editHandler);

        tocHandler = new ToCHandler(this, requireActivity().getWindow().getDecorView().findViewById(R.id.toc_container),
                requireActivity().getWindow().getDecorView().findViewById(R.id.page_scroller_button), bridge);

        // TODO: initialize View references in onCreateView().
        leadImagesHandler = new LeadImagesHandler(this, bridge, webView, pageHeaderView);

        bottomContentView.setup(this, bridge, webView);

        shareHandler = new ShareHandler(this, bridge);

        if (callback() != null) {
            LongPressHandler.WebViewContextMenuListener contextMenuListener
                    = new PageContainerLongPressHandler(this);
            new LongPressHandler(webView, HistoryEntry.SOURCE_INTERNAL_LINK, contextMenuListener);
        }

        pageFragmentLoadState.setUp(model, this, refreshView, webView, bridge, leadImagesHandler, getCurrentTab());

        if (shouldLoadFromBackstack(requireActivity()) || savedInstanceState != null) {
            reloadFromBackstack();
        }
    }

    public void reloadFromBackstack() {
        pageFragmentLoadState.setTab(getCurrentTab());
        if (!pageFragmentLoadState.backStackEmpty()) {
            pageFragmentLoadState.loadFromBackStack();
        } else {
            loadMainPageInForegroundTab();
        }
    }

    void setToolbarFadeEnabled(boolean enabled) {
        if (callback() != null) {
            callback().onPageSetToolbarFadeEnabled(enabled);
        }
    }

    private boolean shouldLoadFromBackstack(@NonNull Activity activity) {
        return activity.getIntent() != null
                && (ACTION_RESUME_READING.equals(activity.getIntent().getAction())
                || activity.getIntent().hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING));
    }

    private void initWebViewListeners() {
        webView.addOnUpOrCancelMotionEventListener(() -> {
            // update our session, since it's possible for the user to remain on the page for
            // a long time, and we wouldn't want the session to time out.
            app.getSessionFunnel().touchSession();
        });
        webView.addOnScrollChangeListener((int oldScrollY, int scrollY, boolean isHumanScroll) -> {
            if (pageScrollFunnel != null) {
                pageScrollFunnel.onPageScrolled(oldScrollY, scrollY, isHumanScroll);
            }
        });
        webView.setWebViewClient(new OkHttpWebViewClient() {
            @NonNull @Override public PageViewModel getModel() {
                return model;
            }
        });
    }

    private void handleInternalLink(@NonNull PageTitle title) {
        if (!isResumed()) {
            return;
        }
        // If it's a Special page, launch it in an external browser, since mobileview
        // doesn't support the Special namespace.
        // TODO: remove when Special pages are properly returned by the server
        // If this is a Talk page also show in external browser since we don't handle those pages
        // in the app very well at this time.
        if (title.isSpecial() || title.isTalkPage()) {
            visitInExternalBrowser(requireActivity(), Uri.parse(title.getMobileUri()));
            return;
        }
        dismissBottomSheet();
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
        if (model.getTitle() != null) {
            historyEntry.setReferrer(model.getTitle().getCanonicalUri());
        }
        if (title.namespace() != Namespace.MAIN || !isLinkPreviewEnabled()) {
            loadPage(title, historyEntry);
        } else {
            Callback callback = callback();
            if (callback != null) {
                callback.onPageShowLinkPreview(historyEntry);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        activeTimer.pause();
        addTimeSpentReading(activeTimer.getElapsedSec());

        pageFragmentLoadState.updateCurrentBackStackItem();
        app.commitTabState();
        closePageScrollFunnel();

        long time = app.getTabList().size() >= 1 && !pageFragmentLoadState.backStackEmpty()
                ? System.currentTimeMillis()
                : 0;
        Prefs.pageLastShown(time);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPageScrollFunnel();
        activeTimer.resume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sendDecorOffsetMessage();
        // if the screen orientation changes, then re-layout the lead image container,
        // but only if we've finished fetching the page.
        if (!pageFragmentLoadState.isLoading() && !errorState) {
            pageFragmentLoadState.layoutLeadImage();
        }
    }

    public Tab getCurrentTab() {
        return app.getTabList().get(app.getTabList().size() - 1);
    }

    private void setCurrentTab(int position, boolean updatePrevBackStackItem) {
        // move the selected tab to the bottom of the list, and navigate to it!
        // (but only if it's a different tab than the one currently in view!
        if (position < app.getTabList().size() - 1) {
            Tab tab = app.getTabList().remove(position);
            app.getTabList().add(tab);
            if (updatePrevBackStackItem) {
                pageFragmentLoadState.updateCurrentBackStackItem();
            }
            pageFragmentLoadState.setTab(tab);
            pageFragmentLoadState.loadFromBackStack();
        }
    }

    public void openInNewBackgroundTabFromMenu(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        if (app.getTabCount() == 0) {
            openInNewForegroundTabFromMenu(title, entry);
        } else {
            openInNewTabFromMenu(title, entry, getBackgroundTabPosition());
            ((PageActivity) requireActivity()).animateTabsButton();
        }
    }

    public void openInNewForegroundTabFromMenu(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        openInNewTabFromMenu(title, entry, getForegroundTabPosition());
        pageFragmentLoadState.loadFromBackStack();
    }

    public void openInNewTabFromMenu(@NonNull PageTitle title, @NonNull HistoryEntry entry, int position) {
        openInNewTab(title, entry, position);
        tabFunnel.logOpenInNew(app.getTabList().size());
    }

    public void openFromExistingTab(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        if (!title.isMainPage() && !title.isFilePage() && app.getTabCount() > 0) {
            PageTitle t = app.getTabList().get(app.getTabList().size() - 1).getBackStackPositionTitle();
            if (t != null) {
                pageHeaderView.loadImage(t.getThumbUrl());
            }
        }

        // find the tab in which this title appears...
        int selectedTabPosition = -1;
        for (Tab tab : app.getTabList()) {
            for (PageBackStackItem item : tab.getBackStack()) {
                if (item.getTitle().equals(title)) {
                    selectedTabPosition = app.getTabList().indexOf(tab);
                    break;
                }
            }
        }
        if (selectedTabPosition == -1) {
            // open the page anyway, in a new tab
            openInNewForegroundTabFromMenu(title, entry);
            return;
        }
        if (selectedTabPosition == app.getTabList().size() - 1) {
            pageFragmentLoadState.loadFromBackStack();
        } else {
            setCurrentTab(selectedTabPosition, false);
        }
    }

    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean pushBackStack) {
        //is the new title the same as what's already being displayed?
        if (!getCurrentTab().getBackStack().isEmpty()
                && getCurrentTab().getBackStack().get(getCurrentTab().getBackStackPosition()).getTitle().equals(title)) {
            if (model.getPage() == null) {
                pageFragmentLoadState.loadFromBackStack();
            } else if (!TextUtils.isEmpty(title.getFragment())) {
                scrollToSection(title.getFragment());
            }
            return;
        }

        loadPage(title, entry, pushBackStack, 0);
    }

    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry,
                         boolean pushBackStack, int stagedScrollY) {
        loadPage(title, entry, pushBackStack, stagedScrollY, false);
    }

    /**
     * Load a new page into the WebView in this fragment.
     * This shall be the single point of entry for loading content into the WebView, whether it's
     * loading an entirely new page, refreshing the current page, retrying a failed network
     * request, etc.
     * @param title Title of the new page to load.
     * @param entry HistoryEntry associated with the new page.
     * @param pushBackStack Whether to push the new page onto the backstack.
     */
    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry,
                         boolean pushBackStack, int stagedScrollY, boolean isRefresh) {
        // clear the title in case the previous page load had failed.
        clearActivityActionBarTitle();

        // update the time spent reading of the current page, before loading the new one
        addTimeSpentReading(activeTimer.getElapsedSec());
        activeTimer.reset();

        // disable sliding of the ToC while sections are loading
        tocHandler.setEnabled(false);

        errorState = false;
        errorView.setVisibility(View.GONE);
        tabLayout.enableAllTabs();

        model.setTitle(title);
        model.setTitleOriginal(title);
        model.setCurEntry(entry);
        model.setReadingListPage(null);
        model.setForceNetwork(isRefresh);

        updateProgressBar(true, true, 0);

        this.pageRefreshed = isRefresh;

        closePageScrollFunnel();
        pageFragmentLoadState.load(pushBackStack, stagedScrollY);
        bottomContentView.hide();
        updateBookmarkAndMenuOptions();
    }

    public Bitmap getLeadImageBitmap() {
        return leadImagesHandler.getLeadImageBitmap();
    }

    /**
     * Update the WebView's base font size, based on the specified font size from the app
     * preferences.
     */
    public void updateFontSize() {
        webView.getSettings().setDefaultFontSize((int) app.getFontSize(requireActivity().getWindow()));
    }

    public void updateBookmarkAndMenuOptions() {
        if (!isAdded()) {
            return;
        }
        pageActionTabsCallback.updateBookmark(model.isInReadingList());
        requireActivity().invalidateOptionsMenu();
    }

    public void updateBookmarkAndMenuOptionsFromDao() {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().findPageInAnyList(getTitle())).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    pageActionTabsCallback.updateBookmark(model.getReadingListPage() != null);
                    requireActivity().invalidateOptionsMenu();
                })
                .subscribe(page -> model.setReadingListPage(page),
                        throwable -> model.setReadingListPage(null)));
    }

    public void onActionModeShown(ActionMode mode) {
        // make sure we have a page loaded, since shareHandler makes references to it.
        if (model.getPage() != null) {
            shareHandler.onTextSelected(mode);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.ACTIVITY_REQUEST_EDIT_SECTION
                && resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            pageFragmentLoadState.backFromEditing(data);
            FeedbackUtil.showMessage(requireActivity(), R.string.edit_saved_successfully);
            // and reload the page...
            loadPage(model.getTitleOriginal(), model.getCurEntry(), false);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL
                && resultCode == RESULT_OK) {
            Prefs.setDescriptionEditTutorialEnabled(false);
            startDescriptionEditActivity();
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT
                && resultCode == RESULT_OK) {
            refreshPage();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.homeAsUp:
                // TODO SEARCH: add up navigation, see also http://developer.android.com/training/implementing-navigation/ancestral.html
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sharePageLink() {
        if (getPage() != null) {
            ShareUtil.shareText(requireActivity(), getPage().getTitle());
        }
    }

    @NonNull public ViewGroup getTabLayout() {
        return tabLayout;
    }

    public View getHeaderView() {
        return pageHeaderView;
    }

    public void showFindInPage() {
        if (model.getPage() == null) {
            return;
        }
        final FindInPageFunnel funnel = new FindInPageFunnel(app, model.getTitle().getWikiSite(),
                model.getPage().getPageProperties().getPageId());
        final FindInPageActionProvider findInPageActionProvider
                = new FindInPageActionProvider(this, funnel);

        startSupportActionMode(new ActionMode.Callback() {
            private final String actionModeTag = "actionModeFindInPage";

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                findInPageActionMode = mode;
                MenuItem menuItem = menu.add(R.string.menu_page_find_in_page);
                menuItem.setActionProvider(findInPageActionProvider);
                menuItem.expandActionView();
                setToolbarElevationEnabled(false);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTag(actionModeTag);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                if (webView == null || !isAdded()) {
                    return;
                }
                findInPageActionMode = null;
                funnel.setPageHeight(webView.getContentHeight());
                funnel.logDone();
                webView.clearMatches();
                showToolbar();
                hideSoftKeyboard();
                setToolbarElevationEnabled(true);
            }
        });
    }

    public boolean closeFindInPage() {
        if (findInPageActionMode != null) {
            findInPageActionMode.finish();
            return true;
        }
        return false;
    }

    /**
     * Scroll to a specific section in the WebView.
     * @param sectionAnchor Anchor link of the section to scroll to.
     */
    public void scrollToSection(@NonNull String sectionAnchor) {
        if (!isAdded() || tocHandler == null) {
            return;
        }
        tocHandler.scrollToSection(sectionAnchor);
    }

    public void onPageLoadComplete() {
        refreshView.setEnabled(true);
        requireActivity().invalidateOptionsMenu();

        setupToC(model, pageFragmentLoadState.isFirstPage());
        editHandler.setPage(model.getPage());
        initPageScrollFunnel();
        bottomContentView.setPage(model.getPage());

        if (model.getReadingListPage() != null) {
            final ReadingListPage page = model.getReadingListPage();
            final PageTitle title = model.getTitle();
            disposables.add(Completable.fromAction(() -> {
                if (!TextUtils.equals(page.thumbUrl(), title.getThumbUrl())
                        || !TextUtils.equals(page.description(), title.getDescription())) {
                    page.thumbUrl(title.getThumbUrl());
                    page.description(title.getDescription());
                    ReadingListDbHelper.instance().updatePage(page);
                }
            }).subscribeOn(Schedulers.io()).subscribe());
        }

        checkAndShowBookmarkOnboarding();
    }

    public void onPageLoadError(@NonNull Throwable caught) {
        if (!isAdded()) {
            return;
        }
        updateProgressBar(false, true, 0);
        refreshView.setRefreshing(false);

        if (pageRefreshed) {
            pageRefreshed = false;
        }

        hidePageContent();
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);

        View contentTopOffset = errorView.findViewById(R.id.view_wiki_error_article_content_top_offset);
        View tabLayoutOffset = errorView.findViewById(R.id.view_wiki_error_article_tab_layout_offset);
        contentTopOffset.setLayoutParams(getContentTopOffsetParams(requireContext()));
        contentTopOffset.setVisibility(View.VISIBLE);
        tabLayoutOffset.setLayoutParams(getTabLayoutOffsetParams());
        tabLayoutOffset.setVisibility(View.VISIBLE);

        disableActionTabs(caught);

        refreshView.setEnabled(!ThrowableUtil.is404(caught));
        errorState = true;
        if (callback() != null) {
            callback().onPageLoadError(getTitle());
        }
    }

    public void refreshPage() {
        refreshPage(0);
    }

    public void refreshPage(int stagedScrollY) {
        if (pageFragmentLoadState.isLoading()) {
            refreshView.setRefreshing(false);
            return;
        }

        errorView.setVisibility(View.GONE);

        tabLayout.enableAllTabs();
        errorState = false;

        model.setCurEntry(new HistoryEntry(model.getTitle(), HistoryEntry.SOURCE_HISTORY));
        loadPage(model.getTitle(), model.getCurEntry(), false, stagedScrollY, true);
    }

    boolean isLoading() {
        return pageFragmentLoadState.isLoading();
    }

    CommunicationBridge getBridge() {
        return bridge;
    }

    private void setupToC(@NonNull PageViewModel model, boolean isFirstPage) {
        tocHandler.setupToC(model.getPage(), model.getTitle().getWikiSite(), isFirstPage);
        tocHandler.setEnabled(true);
    }

    private void setBookmarkIconForPageSavedState(boolean pageSaved) {
        View bookmarkTab = tabLayout.getChildAt(PageActionTab.ADD_TO_READING_LIST.code());
        if (bookmarkTab != null) {
            ((ImageView) bookmarkTab).setImageResource(pageSaved ? R.drawable.ic_bookmark_white_24dp
                    : R.drawable.ic_bookmark_border_white_24dp);
        }
    }

    protected void clearActivityActionBarTitle() {
        FragmentActivity currentActivity = requireActivity();
        if (currentActivity instanceof PageActivity) {
            ((PageActivity) currentActivity).clearActionBarTitle();
        }
    }

    private void openInNewTab(@NonNull PageTitle title, @NonNull HistoryEntry entry, int position) {
        if (shouldCreateNewTab()) {
            // create a new tab
            Tab tab = new Tab();
            boolean isForeground = position == getForegroundTabPosition();
            // if the requested position is at the top, then make its backstack current
            if (isForeground) {
                pageFragmentLoadState.setTab(tab);
            }
            // put this tab in the requested position
            app.getTabList().add(position, tab);
            trimTabCount();
            // add the requested page to its backstack
            tab.getBackStack().add(new PageBackStackItem(title, entry));
            if (!isForeground) {
                loadIntoCache(title);
            }
            requireActivity().invalidateOptionsMenu();
        } else {
            getCurrentTab().getBackStack().add(new PageBackStackItem(title, entry));
        }
    }

    private boolean shouldCreateNewTab() {
        return !getCurrentTab().getBackStack().isEmpty();
    }

    private int getBackgroundTabPosition() {
        return Math.max(0, getForegroundTabPosition() - 1);
    }

    private int getForegroundTabPosition() {
        return app.getTabList().size();
    }

    private void setupMessageHandlers() {
        linkHandler = new LinkHandler(requireActivity()) {
            @Override public void onPageLinkClicked(@NonNull String anchor, @NonNull String linkText) {
                dismissBottomSheet();
                JSONObject payload = new JSONObject();
                try {
                    payload.put("anchor", anchor);
                    payload.put("text", linkText);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                bridge.sendMessage("handleReference", payload);
            }

            @Override public void onInternalLinkClicked(@NonNull PageTitle title) {
                handleInternalLink(title);
            }

            @Override public WikiSite getWikiSite() {
                return model.getTitle().getWikiSite();
            }
        };
        bridge.addListener("linkClicked", linkHandler);

        bridge.addListener("referenceClicked", new ReferenceHandler() {
            @Override
            protected void onReferenceClicked(int selectedIndex, @NonNull List<Reference> adjacentReferences) {

                if (!isAdded()) {
                    Log.d("PageFragment", "Detached from activity, so stopping reference click.");
                    return;
                }

                showBottomSheet(new ReferenceDialog(requireActivity(), selectedIndex, adjacentReferences, linkHandler));
            }
        });
        bridge.addListener("imageClicked", (String messageType, JSONObject messagePayload) -> {
            try {
                String href = decodeURL(messagePayload.getString("href"));
                if (href.startsWith("/wiki/")) {
                    String filename = UriUtil.removeInternalLinkPrefix(href);
                    String fileUrl = null;

                    // Set the lead image url manually if the filename equals to the lead image file name.
                    if (getPage() != null && !TextUtils.isEmpty(getPage().getPageProperties().getLeadImageName())) {
                        String leadImageName = addUnderscores(getPage().getPageProperties().getLeadImageName());
                        String leadImageUrl = getPage().getPageProperties().getLeadImageUrl();
                        if (filename.contains(leadImageName) && leadImageUrl != null) {
                            fileUrl = UriUtil.resolveProtocolRelativeUrl(leadImageUrl);
                        }
                    }

                    WikiSite wiki = model.getTitle().getWikiSite();
                    requireActivity().startActivityForResult(GalleryActivity.newIntent(requireActivity(),
                            model.getTitleOriginal(), filename, fileUrl, wiki,
                            GalleryFunnel.SOURCE_NON_LEAD_IMAGE),
                            Constants.ACTIVITY_REQUEST_GALLERY);
                } else {
                    linkHandler.onUrlClick(href, messagePayload.optString("title"), "");
                }
            } catch (JSONException e) {
                L.logRemoteErrorIfProd(e);
            }
        });
        bridge.addListener("mediaClicked", (String messageType, JSONObject messagePayload) -> {
            try {
                String href = decodeURL(messagePayload.getString("href"));
                String filename = StringUtil.removeUnderscores(UriUtil.removeInternalLinkPrefix(href));
                WikiSite wiki = model.getTitle().getWikiSite();
                requireActivity().startActivityForResult(GalleryActivity.newIntent(requireActivity(),
                        model.getTitleOriginal(), filename, wiki,
                        GalleryFunnel.SOURCE_NON_LEAD_IMAGE),
                        Constants.ACTIVITY_REQUEST_GALLERY);
            } catch (JSONException e) {
                L.logRemoteErrorIfProd(e);
            }
        });
        bridge.addListener("pronunciationClicked", (String messageType, JSONObject messagePayload) -> {
            if (avPlayer == null) {
                avPlayer = new DefaultAvPlayer(new MediaPlayerImplementation());
                avPlayer.init();
            }
            if (avCallback == null) {
                avCallback = new AvCallback();
            }
            if (!avPlayer.isPlaying()) {
                updateProgressBar(true, true, 0);
                avPlayer.play(getPage().getTitlePronunciationUrl(), avCallback, avCallback);
            } else {
                updateProgressBar(false, true, 0);
                avPlayer.stop();
            }
        });
    }

    public void verifyLoggedInThenEditDescription() {
        if (!AccountUtil.isLoggedIn() && Prefs.getTotalAnonDescriptionsEdited() >= getResources().getInteger(R.integer.description_max_anon_edits)) {
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.description_edit_anon_limit)
                    .setPositiveButton(R.string.menu_login, (DialogInterface dialogInterface, int i) ->
                            startActivity(LoginActivity.newIntent(requireContext(), LoginFunnel.SOURCE_EDIT)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            startDescriptionEditActivity();
        }
    }

    private void startDescriptionEditActivity() {
        if (isDescriptionEditTutorialEnabled()) {
            startActivityForResult(DescriptionEditTutorialActivity.newIntent(requireContext()),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL);
        } else {
            startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), getTitle()),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT);
        }
    }

    /**
     * Convenience method for hiding all the content of a page.
     */
    private void hidePageContent() {
        leadImagesHandler.hide();
        webView.setVisibility(View.INVISIBLE);
        if (callback() != null) {
            callback().onPageHideAllContent();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (tocHandler != null && tocHandler.isVisible()) {
            tocHandler.hide();
            return true;
        }
        if (closeFindInPage()) {
            return true;
        }
        if (pageFragmentLoadState.goBack()) {
            return true;
        }
        if (app.getTabList().size() > 1) {
            // if we're at the end of the current tab's backstack, then pop the current tab.
            app.getTabList().remove(app.getTabList().size() - 1);
        }
        return false;
    }

    public void goForward() {
        pageFragmentLoadState.goForward();
    }

    private void checkAndShowBookmarkOnboarding() {
        if (Prefs.shouldShowBookmarkToolTip() && Prefs.getOverflowReadingListsOptionClickCount() == 2) {
            View targetView = tabLayout.getChildAt(PageActionTab.ADD_TO_READING_LIST.code());
            FeedbackUtil.showTapTargetView(requireActivity(), targetView,
                    R.string.tool_tip_bookmark_icon_title, R.string.tool_tip_bookmark_icon_text, null);
            Prefs.shouldShowBookmarkToolTip(false);
        }
    }

    private void sendDecorOffsetMessage() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("offset", getContentTopOffset(requireActivity()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setDecorOffset", payload);
    }

    private void initPageScrollFunnel() {
        if (model.getPage() != null) {
            pageScrollFunnel = new PageScrollFunnel(app, model.getPage().getPageProperties().getPageId());
        }
    }

    private void closePageScrollFunnel() {
        if (pageScrollFunnel != null && webView.getContentHeight() > 0) {
            pageScrollFunnel.setViewportHeight(webView.getHeight());
            pageScrollFunnel.setPageHeight(webView.getContentHeight());
            pageScrollFunnel.logDone();
        }
        pageScrollFunnel = null;
    }

    public void showBottomSheet(@NonNull BottomSheetDialog dialog) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageShowBottomSheet(dialog);
        }
    }

    public void showBottomSheet(@NonNull BottomSheetDialogFragment dialog) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageShowBottomSheet(dialog);
        }
    }

    private void dismissBottomSheet() {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageDismissBottomSheet();
        }
    }

    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageLoadPage(title, entry);
        }
    }

    private void loadMainPageInForegroundTab() {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageLoadMainPageInForegroundTab();
        }
    }

    private void updateProgressBar(boolean visible, boolean indeterminate, int value) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageUpdateProgressBar(visible, indeterminate, value);
        }
    }

    private void showThemeChooser() {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageShowThemeChooser();
        }
    }

    public void startSupportActionMode(@NonNull ActionMode.Callback actionModeCallback) {
        if (callback() != null) {
            callback().onPageStartSupportActionMode(actionModeCallback);
        }
    }

    public void showToolbar() {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageShowToolbar();
        }
    }

    public void hideSoftKeyboard() {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageHideSoftKeyboard();
        }
    }

    public void setToolbarElevationEnabled(boolean enabled) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageSetToolbarElevationEnabled(enabled);
        }
    }

    public void addToReadingList(@NonNull PageTitle title, @NonNull AddToReadingListDialog.InvokeSource source) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageAddToReadingList(title, source);
        }
    }

    public void startLangLinksActivity() {
        Intent langIntent = new Intent();
        langIntent.setClass(requireActivity(), LangLinksActivity.class);
        langIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        langIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, model.getTitle());
        requireActivity().startActivityForResult(langIntent, Constants.ACTIVITY_REQUEST_LANGLINKS);
    }

    private void trimTabCount() {
        while (app.getTabList().size() > Constants.MAX_TABS) {
            app.getTabList().remove(0);
        }
    }

    @SuppressLint("CheckResult")
    private void addTimeSpentReading(int timeSpentSec) {
        if (model.getCurEntry() == null) {
            return;
        }
        model.setCurEntry(new HistoryEntry(model.getCurEntry().getTitle(),
                new Date(),
                model.getCurEntry().getSource(),
                timeSpentSec));
        Completable.fromAction(new UpdateHistoryTask(model.getCurEntry()))
                .subscribeOn(Schedulers.io())
                .subscribe(() -> { }, L::e);
    }

    private LinearLayout.LayoutParams getContentTopOffsetParams(@NonNull Context context) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getContentTopOffsetPx(context));
    }

    private LinearLayout.LayoutParams getTabLayoutOffsetParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, tabLayout.getHeight());
    }

    private void disableActionTabs(@Nullable Throwable caught) {
        boolean offline = isOffline(caught);
        for (int i = 0; i < tabLayout.getChildCount(); i++) {
            if (!(offline && PageActionTab.of(i).equals(PageActionTab.ADD_TO_READING_LIST))) {
                tabLayout.disableTab(i);
            }
        }
    }

    private class AvCallback implements AvPlayer.Callback, AvPlayer.ErrorCallback {
        @Override
        public void onSuccess() {
            if (avPlayer != null) {
                avPlayer.stop();
                updateProgressBar(false, true, 0);
            }
        }
        @Override
        public void onError() {
            if (avPlayer != null) {
                avPlayer.stop();
                updateProgressBar(false, true, 0);
            }
        }
    }

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
