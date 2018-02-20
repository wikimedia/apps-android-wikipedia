package org.wikipedia.page;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
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
import org.wikipedia.analytics.PageScrollFunnel;
import org.wikipedia.analytics.TabFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.UpdateHistoryTask;
import org.wikipedia.language.LangLinksActivity;
import org.wikipedia.offline.OfflineManager;
import org.wikipedia.onboarding.PrefsOnboardingStateMachine;
import org.wikipedia.page.action.PageActionTab;
import org.wikipedia.page.action.PageActionToolbarHideHandler;
import org.wikipedia.page.bottomcontent.BottomContentView;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.shareafact.ShareHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.page.tabs.TabsProvider;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.RemoveFromReadingListsDialog;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.settings.Prefs;
import org.wikipedia.theme.ThemeBridgeAdapter;
import org.wikipedia.util.ActiveTimer;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ConfigurableTabLayout;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;
import org.wikipedia.views.WikiDrawerLayout;
import org.wikipedia.views.WikiPageErrorView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.page.PageActivity.ACTION_RESUME_READING;
import static org.wikipedia.page.PageActivity.ACTION_SHOW_TAB_LIST;
import static org.wikipedia.page.PageCacher.loadIntoCache;
import static org.wikipedia.settings.Prefs.isLinkPreviewEnabled;
import static org.wikipedia.util.DimenUtil.getContentTopOffset;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.util.ResourceUtil.getThemedColor;
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
        void onPageShowLinkPreview(@NonNull PageTitle title, int source);
        void onPageLoadMainPageInForegroundTab();
        void onPageUpdateProgressBar(boolean visible, boolean indeterminate, int value);
        void onPageShowThemeChooser();
        void onPageStartSupportActionMode(@NonNull ActionMode.Callback callback);
        void onPageShowToolbar();
        void onPageHideSoftKeyboard();
        void onPageAddToReadingList(@NonNull PageTitle title,
                                    @NonNull AddToReadingListDialog.InvokeSource source);
        void onPageRemoveFromReadingLists(@NonNull PageTitle title);
        void onPagePopFragment();
        void onPageInvalidateOptionsMenu();
        void onPageLoadError(@NonNull PageTitle title);
        void onPageLoadErrorBackPressed();
        void onPageHideAllContent();
        void onPageSetToolbarFadeEnabled(boolean enabled);
        void onPageSetToolbarForceNoFace(boolean force);
    }

    public static final int TOC_ACTION_SHOW = 0;
    public static final int TOC_ACTION_HIDE = 1;
    public static final int TOC_ACTION_TOGGLE = 2;

    private boolean pageRefreshed;
    private boolean errorState = false;

    private static final int REFRESH_SPINNER_ADDITIONAL_OFFSET = (int) (16 * DimenUtil.getDensityScalar());

    private PageFragmentLoadState pageFragmentLoadState;
    private PageViewModel model;
    private PageInfo pageInfo;

    /**
     * List of tabs, each of which contains a backstack of page titles.
     * Since the list consists of Parcelable objects, it can be saved and restored from the
     * savedInstanceState of the fragment.
     */
    @NonNull private final List<Tab> tabList = new ArrayList<>();

    @NonNull private TabFunnel tabFunnel = new TabFunnel();

    private PageScrollFunnel pageScrollFunnel;
    private LeadImagesHandler leadImagesHandler;
    private BottomContentView bottomContentView;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikiPageErrorView errorView;
    private WikiDrawerLayout tocDrawer;
    private ConfigurableTabLayout tabLayout;
    private ToCHandler tocHandler;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private EditHandler editHandler;
    private ActionMode findInPageActionMode;
    private ShareHandler shareHandler;
    private TabsProvider tabsProvider;
    private ActiveTimer activeTimer = new ActiveTimer();

    private WikipediaApp app;

    @NonNull
    private final SwipeRefreshLayout.OnRefreshListener pageRefreshListener = this::refreshPage;

    @NonNull
    private final TabLayout.OnTabSelectedListener pageActionTabListener
            = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            if (tabLayout.isEnabled(tab)) {
                PageActionTab.of(tab.getPosition()).select(pageActionTabsCallback);
            }
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            onTabSelected(tab);
        }
    };

    private PageActionTab.Callback pageActionTabsCallback = new PageActionTab.Callback() {
        @Override
        public void onAddToReadingListTabSelected() {
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
        public void onViewToCTabSelected() {
            toggleToC(TOC_ACTION_TOGGLE);
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

    public boolean isPresentInOfflineLists() {
        return model.isInReadingList();
    }

    public PageTitle getTitleOriginal() {
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

    public boolean getErrorState() {
        return errorState;
    }

    public BottomContentView getBottomContentView() {
        return bottomContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getActivity().getApplicationContext();
        model = new PageViewModel();

        pageFragmentLoadState = new PageFragmentLoadState();

        initTabs();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);

        webView = rootView.findViewById(R.id.page_web_view);
        initWebViewListeners();

        tocDrawer = rootView.findViewById(R.id.page_toc_drawer);
        tocDrawer.setDragEdgeWidth(getResources().getDimensionPixelSize(R.dimen.drawer_drag_margin));

        refreshView = rootView.findViewById(R.id.page_refresh_container);
        int swipeOffset = getContentTopOffsetPx(getActivity()) + REFRESH_SPINNER_ADDITIONAL_OFFSET;
        refreshView.setProgressViewOffset(false, -swipeOffset, swipeOffset);
        refreshView.setColorSchemeResources(getThemedAttributeId(getContext(), R.attr.colorAccent));
        refreshView.setScrollableChild(webView);
        refreshView.setOnRefreshListener(pageRefreshListener);

        tabLayout = rootView.findViewById(R.id.page_actions_tab_layout);
        tabLayout.addOnTabSelectedListener(pageActionTabListener);

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
        //uninitialize the bridge, so that no further JS events can have any effect.
        bridge.cleanup();
        tabsProvider.setTabsProviderListener(null);
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
        webView.setBackgroundColor(getThemedColor(getActivity(), R.attr.paper_color));

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        setupMessageHandlers();
        sendDecorOffsetMessage();

        if (!app.getCurrentTheme().isDefault()) {
            ThemeBridgeAdapter.setTheme(bridge);
        }

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

        tocHandler = new ToCHandler(this, tocDrawer, bridge);

        // TODO: initialize View references in onCreateView().
        leadImagesHandler = new LeadImagesHandler(this, bridge, webView,
                getView().findViewById(R.id.page_header_view));

        bottomContentView.setup(this, bridge, webView);

        shareHandler = new ShareHandler(this, bridge);
        tabsProvider = new TabsProvider(this, tabList);
        tabsProvider.setTabsProviderListener(tabsProviderListener);

        if (callback() != null) {
            LongPressHandler.WebViewContextMenuListener contextMenuListener
                    = new PageContainerLongPressHandler(this);
            new LongPressHandler(webView, HistoryEntry.SOURCE_INTERNAL_LINK, contextMenuListener);
        }

        pageFragmentLoadState.setUp(model, this, refreshView, webView, bridge, leadImagesHandler, getCurrentTab().getBackStack());

        if (shouldLoadFromBackstack(getActivity()) || savedInstanceState != null) {
            if (!pageFragmentLoadState.backStackEmpty()) {
                pageFragmentLoadState.loadFromBackStack();
            } else {
                loadMainPageInForegroundTab();
            }
        }

        if (shouldShowTabList(getActivity())) {
            showTabList();
        }
    }

    public void setToolbarForceNoFace(boolean force) {
        if (callback() != null) {
            callback().onPageSetToolbarForceNoFace(force);
        }
    }

    void setToolbarFadeEnabled(boolean enabled) {
        if (callback() != null) {
            callback().onPageSetToolbarFadeEnabled(enabled);
        }
    }

    private boolean shouldLoadFromBackstack(@NonNull Activity activity) {
        return activity.getIntent() != null
                && (ACTION_SHOW_TAB_LIST.equals(activity.getIntent().getAction())
                || ACTION_RESUME_READING.equals(activity.getIntent().getAction())
                || activity.getIntent().hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING));
    }

    private boolean shouldShowTabList(@NonNull Activity activity) {
        return activity.getIntent() != null
                && ACTION_SHOW_TAB_LIST.equals(activity.getIntent().getAction());
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
            @NonNull @Override public WikiSite getWikiSite() {
                return model.getTitle().getWikiSite();
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
            visitInExternalBrowser(getActivity(), Uri.parse(title.getMobileUri()));
            return;
        }
        dismissBottomSheet();
        if (title.namespace() != Namespace.MAIN || !isLinkPreviewEnabled()
                || (!DeviceUtil.isOnline() && OfflineManager.instance().titleExists(title.getDisplayText()))) {
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
            loadPage(title, historyEntry);
        } else {
            showLinkPreview(title, HistoryEntry.SOURCE_INTERNAL_LINK);
        }
    }

    private TabsProvider.TabsProviderListener tabsProviderListener = new TabsProvider.TabsProviderListener() {
        @Override
        public void onEnterTabView() {
            tabFunnel = new TabFunnel();
            tabFunnel.logEnterList(tabList.size());
            leadImagesHandler.setAnimationPaused(true);
        }

        @Override
        public void onCancelTabView() {
            tabsProvider.exitTabMode();
            tabFunnel.logCancel(tabList.size());
            leadImagesHandler.setAnimationPaused(false);
            if (tabsProvider.shouldPopFragment()) {
                Callback callback = callback();
                if (callback != null) {
                    callback.onPagePopFragment();
                }
            }
        }

        @Override
        public void onTabSelected(int position) {
            setCurrentTab(position, true);
            tabsProvider.exitTabMode();
            tabFunnel.logSelect(tabList.size(), position);
            leadImagesHandler.setAnimationPaused(false);
        }

        @Override
        public void onNewTabRequested() {
            // just load the main page into a new tab...
            loadMainPageInForegroundTab();
            tabFunnel.logCreateNew(tabList.size());

            // Set the current tab to the new opened tab
            tabsProvider.exitTabMode();
            leadImagesHandler.setAnimationPaused(false);
        }

        @Override
        public void onCloseTabRequested(int position) {
            if (!ReleaseUtil.isDevRelease() && (position < 0 || position >= tabList.size())) {
                // According to T109998, the position may possibly be out-of-bounds, but we can't
                // reproduce it. We'll handle this case, but only for non-dev builds, so that we
                // can investigate the issue further if we happen upon it ourselves.
                return;
            }
            tabList.remove(position);
            tabFunnel.logClose(tabList.size(), position);
            tabsProvider.invalidate();
            getActivity().invalidateOptionsMenu();
            if (tabList.size() == 0) {
                tabFunnel.logCancel(tabList.size());
                tabsProvider.exitTabMode();
                // and if the last tab was closed, then finish the activity!
                if (!tabsProvider.shouldPopFragment()) {
                    getActivity().finish();
                }
            } else if (position == tabList.size()) {
                // if it's the topmost tab, then load the topmost page in the next tab.
                pageFragmentLoadState.setBackStack(getCurrentTab().getBackStack());
                pageFragmentLoadState.loadFromBackStack();
            }
        }

        @Override
        public void onCloseAllTabs() {
            tabList.clear();
            Prefs.clearTabs();
            getActivity().finish();
        }
    };

    @Override
    public void onPause() {
        super.onPause();

        activeTimer.pause();
        addTimeSpentReading(activeTimer.getElapsedSec());

        pageFragmentLoadState.updateCurrentBackStackItem();
        Prefs.setTabs(tabList);
        closePageScrollFunnel();

        long time = tabList.size() >= 1 && !pageFragmentLoadState.backStackEmpty()
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
        tabsProvider.onConfigurationChanged();
    }

    public Tab getCurrentTab() {
        return tabList.get(tabList.size() - 1);
    }

    public void invalidateTabs() {
        tabsProvider.invalidate();
    }

    private void setCurrentTab(int position, boolean updatePrevBackStackItem) {
        // move the selected tab to the bottom of the list, and navigate to it!
        // (but only if it's a different tab than the one currently in view!
        if (position < tabList.size() - 1) {
            Tab tab = tabList.remove(position);
            tabList.add(tab);
            tabsProvider.invalidate();
            if (updatePrevBackStackItem) {
                pageFragmentLoadState.updateCurrentBackStackItem();
            }
            pageFragmentLoadState.setBackStack(tab.getBackStack());
            pageFragmentLoadState.loadFromBackStack();
        }
    }

    public void openInNewBackgroundTabFromMenu(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        if (noPagesOpen()) {
            openInNewForegroundTabFromMenu(title, entry);
        } else {
            openInNewTabFromMenu(title, entry, getBackgroundTabPosition());
            tabsProvider.showAndHideTabs();
        }
    }

    public void openInNewForegroundTabFromMenu(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        openInNewTabFromMenu(title, entry, getForegroundTabPosition());
        pageFragmentLoadState.loadFromBackStack();
    }

    public void openInNewTabFromMenu(@NonNull PageTitle title, @NonNull HistoryEntry entry, int position) {
        openInNewTab(title, entry, position);
        tabFunnel.logOpenInNew(tabList.size());
    }

    public void openFromExistingTab(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        // find the tab in which this title appears...
        int selectedTabPosition = -1;
        for (Tab tab : tabList) {
            for (PageBackStackItem item : tab.getBackStack()) {
                if (item.getTitle().equals(title)) {
                    selectedTabPosition = tabList.indexOf(tab);
                    break;
                }
            }
        }
        if (selectedTabPosition == -1) {
            // open the page anyway, in a new tab
            openInNewForegroundTabFromMenu(title, entry);
            return;
        }
        if (selectedTabPosition == tabList.size() - 1) {
            pageFragmentLoadState.loadFromBackStack();
        } else {
            setCurrentTab(selectedTabPosition, false);
        }
    }

    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean pushBackStack) {
        //is the new title the same as what's already being displayed?
        if (!getCurrentTab().getBackStack().isEmpty()
                && getCurrentTab().getBackStack().get(getCurrentTab().getBackStack().size() - 1)
                .getTitle().equals(title)) {
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

        leadImagesHandler.hide();

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
        webView.getSettings().setDefaultFontSize((int) app.getFontSize(getActivity().getWindow()));
    }

    public void updateBookmarkAndMenuOptions() {
        if (!isAdded()) {
            return;
        }
        pageActionTabsCallback.updateBookmark(model.isInReadingList());
        if (callback() != null) {
            callback().onPageInvalidateOptionsMenu();
        }
    }

    public void updateBookmarkAndMenuOptionsFromDao() {
        CallbackTask.execute(() -> ReadingListDbHelper.instance().findPageInAnyList(getTitle()), new CallbackTask.DefaultCallback<ReadingListPage>() {
            @Override
            public void success(ReadingListPage page) {
                if (!isAdded()) {
                    return;
                }
                model.setReadingListPage(page);
                pageActionTabsCallback.updateBookmark(page != null);
                if (callback() != null) {
                    callback().onPageInvalidateOptionsMenu();
                }
            }
        });
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
            FeedbackUtil.showMessage(getActivity(), R.string.edit_saved_successfully);
            // and reload the page...
            loadPage(model.getTitleOriginal(), model.getCurEntry(), false);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL
                && resultCode == RESULT_OK) {
            PrefsOnboardingStateMachine.getInstance().setDescriptionEditTutorial();
            startDescriptionEditActivity();
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT
                && resultCode == RESULT_OK) {
            refreshPage();
        }
    }

    public void startDescriptionEditActivity() {
        startActivityForResult(DescriptionEditActivity.newIntent(getContext(), getTitle()),
                Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.homeAsUp:
                // TODO SEARCH: add up navigation, see also http://developer.android.com/training/implementing-navigation/ancestral.html
                return true;
            case R.id.menu_page_other_languages:
                startLangLinksActivity();
                return true;
            case R.id.menu_page_share:
                sharePageLink();
                return true;
            case R.id.menu_page_add_to_list:
                addToReadingList(getTitle(), AddToReadingListDialog.InvokeSource.PAGE_OVERFLOW_MENU);
                return true;
            case R.id.menu_page_remove_from_list:
                showRemoveFromListsDialog();
                return true;
            case R.id.menu_page_find_in_page:
                showFindInPage();
                return true;
            case R.id.menu_page_content_issues:
                showContentIssues();
                return true;
            case R.id.menu_page_similar_titles:
                showSimilarTitles();
                return true;
            case R.id.menu_page_font_and_theme:
                showThemeChooser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showRemoveFromListsDialog() {
        CallbackTask.execute(() -> {
            List<ReadingListPage> pageOccurrences = ReadingListDbHelper.instance().getAllPageOccurrences(model.getTitle());
            return ReadingListDbHelper.instance().getListsFromPageOccurrences(pageOccurrences);
        }, new CallbackTask.DefaultCallback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> listsContainingPage) {
                if (!isAdded()) {
                    return;
                }
                new RemoveFromReadingListsDialog(listsContainingPage).deleteOrShowDialog(getContext(),
                        page -> {
                            if (callback() != null) {
                                callback().onPageRemoveFromReadingLists(getTitle());
                            }
                        });
            }
        });
    }

    public void sharePageLink() {
        if (getPage() != null) {
            ShareUtil.shareText(getActivity(), getPage().getTitle());
        }
    }

    @NonNull public TabLayout getTabLayout() {
        return tabLayout;
    }

    public int getTabCount() {
        return tabList.size();
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
                findInPageActionMode = null;
                funnel.setPageHeight(webView.getContentHeight());
                funnel.logDone();
                webView.clearMatches();
                showToolbar();
                hideSoftKeyboard();
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
        if (callback() != null) {
            callback().onPageInvalidateOptionsMenu();
        }
        setupToC(model, pageFragmentLoadState.isFirstPage());
        editHandler.setPage(model.getPage());
        initPageScrollFunnel();
        bottomContentView.setPage(model.getPage());

        if (model.getReadingListPage() != null) {
            final ReadingListPage page = model.getReadingListPage();
            final PageTitle title = model.getTitle();
            CallbackTask.execute(() -> {
                if (!TextUtils.equals(page.thumbUrl(), title.getThumbUrl())
                        || !TextUtils.equals(page.description(), title.getDescription())) {
                    page.thumbUrl(title.getThumbUrl());
                    page.description(title.getDescription());
                    ReadingListDbHelper.instance().updatePage(page);
                }
            });
        }

        checkAndShowSelectTextOnboarding();
    }

    public void onPageLoadError(@NonNull Throwable caught) {
        if (!isAdded()) {
            return;
        }
        // in any case, make sure the TOC drawer is closed
        tocDrawer.closeDrawers();
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
        contentTopOffset.setLayoutParams(getContentTopOffsetParams(getContext()));
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

    public void toggleToC(int action) {
        // tocHandler could still be null while the page is loading
        if (tocHandler == null) {
            return;
        }
        switch (action) {
            case TOC_ACTION_SHOW:
                tocHandler.show();
                break;
            case TOC_ACTION_HIDE:
                tocHandler.hide();
                break;
            case TOC_ACTION_TOGGLE:
                if (tocHandler.isVisible()) {
                    tocHandler.hide();
                } else {
                    tocHandler.show();
                }
                break;
            default:
                throw new RuntimeException("Unknown action!");
        }
    }

    PageInfo getPageInfo() {
        return pageInfo;
    }

    boolean isLoading() {
        return pageFragmentLoadState.isLoading();
    }

    CommunicationBridge getBridge() {
        return bridge;
    }

    void enterTabMode(boolean launchedExternally) {
        tabsProvider.enterTabMode(launchedExternally);
    }

    private void setupToC(@NonNull PageViewModel model, boolean isFirstPage) {
        tocHandler.setupToC(model.getPage(), model.getTitle().getWikiSite(), isFirstPage);
        tocHandler.setEnabled(true);
    }

    private void setBookmarkIconForPageSavedState(boolean pageSaved) {
        TabLayout.Tab bookmarkTab = tabLayout.getTabAt(PageActionTab.ADD_TO_READING_LIST.code());
        if (bookmarkTab != null) {
            bookmarkTab.setIcon(pageSaved ? R.drawable.ic_bookmark_white_24dp
                    : R.drawable.ic_bookmark_border_white_24dp);
        }
    }

    private void showContentIssues() {
        showPageInfoDialog(false);
    }

    private void showSimilarTitles() {
        showPageInfoDialog(true);
    }

    private void showPageInfoDialog(boolean startAtDisambig) {
        showBottomSheet(new PageInfoDialog(this, pageInfo, startAtDisambig));
    }

    private void showTabList() {
        // Doesn't seem to be a way around doing a post() here...
        // Without post(), the tab picker layout is inflated with wrong dimensions.
        webView.post(() -> tabsProvider.enterTabMode(true));
    }

    protected void clearActivityActionBarTitle() {
        FragmentActivity currentActivity = getActivity();
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
                pageFragmentLoadState.setBackStack(tab.getBackStack());
            }
            // put this tab in the requested position
            tabList.add(position, tab);
            trimTabCount();
            tabsProvider.invalidate();
            // add the requested page to its backstack
            tab.getBackStack().add(new PageBackStackItem(title, entry));
            if (!isForeground) {
                loadIntoCache(title);
            }
            getActivity().invalidateOptionsMenu();
        } else {
            getTopMostTab().getBackStack().add(new PageBackStackItem(title, entry));
        }
    }

    private boolean noPagesOpen() {
        return tabList.isEmpty()
                || (tabList.size() == 1 && tabList.get(0).getBackStack().isEmpty());
    }

    private Tab getTopMostTab() {
        return tabList.get(tabList.size() - 1);
    }

    private boolean shouldCreateNewTab() {
        return !getTopMostTab().getBackStack().isEmpty();
    }

    private int getBackgroundTabPosition() {
        return Math.max(0, getForegroundTabPosition() - 1);
    }

    private int getForegroundTabPosition() {
        return tabList.size();
    }

    private void setupMessageHandlers() {
        linkHandler = new LinkHandler(getActivity()) {
            @Override public void onPageLinkClicked(@NonNull String anchor) {
                dismissBottomSheet();
                JSONObject payload = new JSONObject();
                try {
                    payload.put("anchor", anchor);
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
            protected void onReferenceClicked(@NonNull String refHtml, @Nullable String refLinkText) {
                if (!isAdded()) {
                    Log.d("PageFragment", "Detached from activity, so stopping reference click.");
                    return;
                }
                showBottomSheet(new ReferenceDialog(getActivity(), linkHandler, refHtml,
                        StringUtils.defaultString(refLinkText)));
            }
        });
        bridge.addListener("ipaSpan", (String messageType, JSONObject messagePayload) -> {
            try {
                String text = messagePayload.getString("contents");
                final int textSize = 30;
                TextView textView = new TextView(getActivity());
                textView.setGravity(Gravity.CENTER);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                textView.setText(StringUtil.fromHtml(text));
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setView(textView);
                builder.show();
            } catch (JSONException e) {
                L.logRemoteErrorIfProd(e);
            }
        });
        bridge.addListener("imageClicked", (String messageType, JSONObject messagePayload) -> {
            try {
                String href = decodeURL(messagePayload.getString("href"));
                if (href.startsWith("/wiki/")) {
                    String filename = UriUtil.removeInternalLinkPrefix(href);
                    WikiSite wiki = model.getTitle().getWikiSite();
                    getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                            model.getTitleOriginal(), filename, wiki,
                            GalleryFunnel.SOURCE_NON_LEAD_IMAGE),
                            Constants.ACTIVITY_REQUEST_GALLERY);
                } else {
                    linkHandler.onUrlClick(href, messagePayload.optString("title"));
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
                getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                        model.getTitleOriginal(), filename, wiki,
                        GalleryFunnel.SOURCE_NON_LEAD_IMAGE),
                        Constants.ACTIVITY_REQUEST_GALLERY);
            } catch (JSONException e) {
                L.logRemoteErrorIfProd(e);
            }
        });
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
        if (pageFragmentLoadState.popBackStack()) {
            return true;
        }
        if (tabsProvider.onBackPressed()) {
            return true;
        }
        if (tabList.size() > 1) {
            // if we're at the end of the current tab's backstack, then pop the current tab.
            tabList.remove(tabList.size() - 1);
            tabsProvider.invalidate();
        }
        return false;
    }

    public void updatePageInfo(@Nullable PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void checkAndShowSelectTextOnboarding() {
        if (model.getPage().isArticle()
                &&  PrefsOnboardingStateMachine.getInstance().isSelectTextTutorialEnabled()) {
            showSelectTextOnboarding();
        }
    }

    private void showSelectTextOnboarding() {
        final View targetView = getView().findViewById(R.id.fragment_page_tool_tip_select_text_target);
        FeedbackUtil.showTapTargetView(getActivity(), targetView,
                R.string.tool_tip_select_text_title, R.string.tool_tip_select_text, null);
        PrefsOnboardingStateMachine.getInstance().setSelectTextTutorial();
    }

    private void initTabs() {
        if (Prefs.hasTabs()) {
            tabList.addAll(Prefs.getTabs());
        }

        if (tabList.isEmpty()) {
            tabList.add(new Tab());
        }
    }

    private void sendDecorOffsetMessage() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("offset", getContentTopOffset(getActivity()));
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

    private void showLinkPreview(@NonNull PageTitle title, int source) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageShowLinkPreview(title, source);
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

    public void addToReadingList(@NonNull PageTitle title, @NonNull AddToReadingListDialog.InvokeSource source) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageAddToReadingList(title, source);
        }
    }

    public View getTabsContainerView() {
        return getActivity().findViewById(R.id.tabs_container);
    }

    public void startLangLinksActivity() {
        Intent langIntent = new Intent();
        langIntent.setClass(getActivity(), LangLinksActivity.class);
        langIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        langIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, model.getTitle());
        getActivity().startActivityForResult(langIntent, Constants.ACTIVITY_REQUEST_LANGLINKS);
    }

    private void trimTabCount() {
        while (tabList.size() > Constants.MAX_TABS) {
            tabList.remove(0);
        }
    }

    private void addTimeSpentReading(int timeSpentSec) {
        if (model.getCurEntry() == null) {
            return;
        }
        model.setCurEntry(new HistoryEntry(model.getCurEntry().getTitle(),
                new Date(),
                model.getCurEntry().getSource(),
                timeSpentSec));
        new UpdateHistoryTask(model.getCurEntry(), app).execute();
    }

    private LinearLayout.LayoutParams getContentTopOffsetParams(@NonNull Context context) {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getContentTopOffsetPx(context));
    }

    private LinearLayout.LayoutParams getTabLayoutOffsetParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, tabLayout.getHeight());
    }

    private void disableActionTabs(@Nullable Throwable caught) {
        boolean offline = caught != null && isOffline(caught);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            if (!(offline && PageActionTab.of(i).equals(PageActionTab.ADD_TO_READING_LIST))) {
                tabLayout.disableTab(i);
            }
        }
    }

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
