package org.wikipedia.page;

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
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.appenguin.onboarding.ToolTip;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.LongPressHandler;
import org.wikipedia.NightModeHandler;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.FindInPageFunnel;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.PageScrollFunnel;
import org.wikipedia.analytics.TabFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.StyleBundle;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.page.action.PageActionTab;
import org.wikipedia.page.action.PageActionToolbarHideHandler;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.leadimages.ArticleHeaderView;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.snippet.CompatActionMode;
import org.wikipedia.page.snippet.ShareHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.page.tabs.TabsProvider;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingList;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.savedpages.ImageUrlMap;
import org.wikipedia.savedpages.LoadSavedPageUrlMapTask;
import org.wikipedia.settings.Prefs;
import org.wikipedia.tooltip.ToolTipUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;
import org.wikipedia.views.WikiDrawerLayout;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static butterknife.ButterKnife.findById;
import static org.wikipedia.util.DimenUtil.getContentTopOffset;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.util.UriUtil.decodeURL;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class PageFragment extends Fragment implements BackPressedHandler {
    public interface Callback {
        void onPageShowBottomSheet(@NonNull BottomSheetDialog dialog);
        void onPageShowBottomSheet(@NonNull BottomSheetDialogFragment dialog);
        void onPageDismissBottomSheet();
        @Nullable PageToolbarHideHandler onPageGetToolbarHideHandler();
        void onPageLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry);
        void onPageLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry,
                            @NonNull TabsProvider.TabPosition tabPosition);
        void onPageShowLinkPreview(@NonNull PageTitle title, int source);
        void onPageLoadMainPageInForegroundTab();
        void onPageUpdateProgressBar(boolean visible, boolean indeterminate, int value);
        void onPageSearchRequested();
        boolean onPageIsSearching();
        @Nullable Fragment onPageGetTopFragment();
        void onPageShowThemeChooser();
        @Nullable ActionMode onPageStartSupportActionMode(@NonNull ActionMode.Callback callback);
        void onPageShowToolbar();
        void onPageHideSoftKeyboard();
        @Nullable PageLoadCallbacks onPageGetPageLoadCallbacks();
        void onPageAddToReadingList(@NonNull PageTitle title,
                                    @NonNull AddToReadingListDialog.InvokeSource source);
        @Nullable View onPageGetContentView();
        @Nullable View onPageGetTabsContainerView();
        void onPagePopFragment();
        @Nullable AppCompatActivity getActivity();
        void onPageInvalidateOptionsMenu();
    }

    public static final int TOC_ACTION_SHOW = 0;
    public static final int TOC_ACTION_HIDE = 1;
    public static final int TOC_ACTION_TOGGLE = 2;

    private boolean pageRefreshed;
    private boolean errorState = false;

    private static final int REFRESH_SPINNER_ADDITIONAL_OFFSET = (int) (16 * DimenUtil.getDensityScalar());

    private PageLoadStrategy pageLoadStrategy;
    private PageViewModel model;
    @Nullable private PageInfo pageInfo;

    /**
     * List of tabs, each of which contains a backstack of page titles.
     * Since the list consists of Parcelable objects, it can be saved and restored from the
     * savedInstanceState of the fragment.
     */
    @NonNull
    private final List<Tab> tabList = new ArrayList<>();

    @NonNull
    private TabFunnel tabFunnel = new TabFunnel();

    @Nullable
    private PageScrollFunnel pageScrollFunnel;
    private LeadImagesHandler leadImagesHandler;
    private PageToolbarHideHandler toolbarHideHandler;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikiErrorView errorView;
    private WikiDrawerLayout tocDrawer;
    private TabLayout tabLayout;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private EditHandler editHandler;
    private ActionMode findInPageActionMode;
    @NonNull private ShareHandler shareHandler;
    private TabsProvider tabsProvider;

    private WikipediaApp app;

    @NonNull
    private final SwipeRefreshLayout.OnRefreshListener pageRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshPage();
        }
    };

    @NonNull
    private final TabLayout.OnTabSelectedListener pageActionTabListener
            = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            PageActionTab.of(tab.getPosition()).select(pageActionTabsCallback);
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
            addToReadingList(AddToReadingListDialog.InvokeSource.BOOKMARK_BUTTON);
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getActivity().getApplicationContext();
        model = new PageViewModel();
        pageLoadStrategy = new JsonPageLoadStrategy();

        initTabs();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);

        webView = (ObservableWebView) rootView.findViewById(R.id.page_web_view);
        initWebViewListeners();

        tocDrawer = (WikiDrawerLayout) rootView.findViewById(R.id.page_toc_drawer);
        tocDrawer.setDragEdgeWidth(getResources().getDimensionPixelSize(R.dimen.drawer_drag_margin));

        refreshView = (SwipeRefreshLayoutWithScroll) rootView
                .findViewById(R.id.page_refresh_container);
        int swipeOffset = getContentTopOffsetPx(getActivity()) + REFRESH_SPINNER_ADDITIONAL_OFFSET;
        refreshView.setProgressViewOffset(false, -swipeOffset, swipeOffset);
        refreshView.setColorSchemeResources(R.color.foundation_blue);
        refreshView.setScrollableChild(webView);
        refreshView.setOnRefreshListener(pageRefreshListener);

        tabLayout = (TabLayout) rootView.findViewById(R.id.page_actions_tab_layout);
        tabLayout.addOnTabSelectedListener(pageActionTabListener);

        PageActionToolbarHideHandler pageActionToolbarHideHandler = new PageActionToolbarHideHandler(tabLayout);
        pageActionToolbarHideHandler.setScrollView(webView);

        errorView = (WikiErrorView)rootView.findViewById(R.id.page_error);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        //uninitialize the bridge, so that no further JS events can have any effect.
        bridge.cleanup();
        tabsProvider.setTabsProviderListener(null);
        toolbarHideHandler.setScrollView(null);
        webView.clearAllListeners();
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        updateFontSize();

        // Explicitly set background color of the WebView (independently of CSS, because
        // the background may be shown momentarily while the WebView loads content,
        // creating a seizure-inducing effect, or at the very least, a migraine with aura).
        webView.setBackgroundColor(getResources().getColor(
                getThemedAttributeId(getActivity(), R.attr.page_background_color)));

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        setupMessageHandlers();
        sendDecorOffsetMessage();

        linkHandler = new LinkHandler(getActivity(), bridge) {
            @Override
            public void onPageLinkClicked(String anchor) {
                dismissBottomSheet();
                JSONObject payload = new JSONObject();
                try {
                    payload.put("anchor", anchor);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                bridge.sendMessage("handleReference", payload);
            }

            @Override
            public void onInternalLinkClicked(PageTitle title) {
                handleInternalLink(title);
            }

            @Override
            public Site getSite() {
                return model.getTitle().getSite();
            }
        };

        new ReferenceHandler(bridge) {
            @Override
            protected void onReferenceClicked(String refHtml) {
                if (!isAdded()) {
                    Log.d("PageFragment", "Detached from activity, so stopping reference click.");
                    return;
                }
                showBottomSheet(new ReferenceDialog(getActivity(), linkHandler, refHtml));
            }
        };

        bridge.injectStyleBundle(StyleBundle.getAvailableBundle(StyleBundle.BUNDLE_PAGEVIEW));

        // make sure styles get injected before the NightModeHandler and other handlers
        if (app.isCurrentThemeDark()) {
            new NightModeHandler(bridge).turnOn(true);
        }

        errorView.setRetryClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshPage();
            }
        });
        errorView.setBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        editHandler = new EditHandler(this, bridge);
        pageLoadStrategy.setEditHandler(editHandler);

        tocHandler = new ToCHandler(this, tocDrawer, bridge);

        // TODO: initialize View references in onCreateView().
        ArticleHeaderView articleHeaderView = findById(getView(), R.id.page_header_view);
        leadImagesHandler = new LeadImagesHandler(this, bridge, webView, articleHeaderView);
        toolbarHideHandler = getSearchBarHideHandler();
        toolbarHideHandler.setScrollView(webView);

        shareHandler = new ShareHandler(this, bridge);
        tabsProvider = new TabsProvider(this, tabList);
        tabsProvider.setTabsProviderListener(tabsProviderListener);

        if (callback() != null) {
            LongPressHandler.WebViewContextMenuListener contextMenuListener
                    = new PageFragmentLongPressHandler(callback());
            new LongPressHandler(getActivity(), webView, HistoryEntry.SOURCE_INTERNAL_LINK,
                    contextMenuListener);
        }

        pageLoadStrategy.setUp(model, this, refreshView, webView, bridge, toolbarHideHandler,
                leadImagesHandler, getCurrentTab().getBackStack());
    }

    private void initWebViewListeners() {
        webView.addOnUpOrCancelMotionEventListener(new ObservableWebView.OnUpOrCancelMotionEventListener() {
            @Override
            public void onUpOrCancelMotionEvent() {
                // update our session, since it's possible for the user to remain on the page for
                // a long time, and we wouldn't want the session to time out.
                app.getSessionFunnel().touchSession();
            }
        });
        webView.addOnScrollChangeListener(new ObservableWebView.OnScrollChangeListener() {
            @Override
            public void onScrollChanged(int oldScrollY, int scrollY, boolean isHumanScroll) {
                if (pageScrollFunnel != null) {
                    pageScrollFunnel.onPageScrolled(oldScrollY, scrollY, isHumanScroll);
                }
            }
        });
    }

    private void handleInternalLink(PageTitle title) {
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
        if (title.namespace() != Namespace.MAIN || !app.isLinkPreviewEnabled()) {
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
            // move the selected tab to the bottom of the list, and navigate to it!
            // (but only if it's a different tab than the one currently in view!
            if (position != tabList.size() - 1) {
                Tab tab = tabList.remove(position);
                tabList.add(tab);
                tabsProvider.invalidate();
                pageLoadStrategy.updateCurrentBackStackItem();
                pageLoadStrategy.setBackStack(tab.getBackStack());
                pageLoadStrategy.loadFromBackStack();
            }
            tabsProvider.exitTabMode();
            tabFunnel.logSelect(tabList.size(), position);
            leadImagesHandler.setAnimationPaused(false);
        }

        @Override
        public void onNewTabRequested() {
            // just load the main page into a new tab...
            loadMainPageInForegroundTab();
            tabFunnel.logCreateNew(tabList.size());
        }

        @Override
        public void onCloseTabRequested(int position) {
            if (!app.isDevRelease() && (position < 0 || position >= tabList.size())) {
                // According to T109998, the position may possibly be out-of-bounds, but we can't
                // reproduce it. We'll handle this case, but only for non-dev builds, so that we
                // can investigate the issue further if we happen upon it ourselves.
                return;
            }
            tabList.remove(position);
            tabFunnel.logClose(tabList.size(), position);
            tabsProvider.invalidate();
            getActivity().supportInvalidateOptionsMenu();
            if (tabList.size() == 0) {
                tabFunnel.logCancel(tabList.size());
                tabsProvider.exitTabMode();
                // and if the last tab was closed, then finish the activity!
                if (!tabsProvider.shouldPopFragment()) {
                    getActivity().finish();
                }
            } else if (position == tabList.size()) {
                // if it's the topmost tab, then load the topmost page in the next tab.
                pageLoadStrategy.setBackStack(getCurrentTab().getBackStack());
                pageLoadStrategy.loadFromBackStack();
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        pageLoadStrategy.updateCurrentBackStackItem();
        Prefs.setTabs(tabList);
        closePageScrollFunnel();

        long time = tabList.size() >= 1 && !pageLoadStrategy.backStackEmpty()
                ? System.currentTimeMillis()
                : 0;
        Prefs.pageLastShown(time);
    }

    @Override
    public void onResume() {
        super.onResume();
        initPageScrollFunnel();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        sendDecorOffsetMessage();
        // if the screen orientation changes, then re-layout the lead image container,
        // but only if we've finished fetching the page.
        if (!pageLoadStrategy.isLoading() && !errorState) {
            pageLoadStrategy.layoutLeadImage();
        }
        tabsProvider.onConfigurationChanged();
    }

    public Tab getCurrentTab() {
        return tabList.get(tabList.size() - 1);
    }

    public void invalidateTabs() {
        tabsProvider.invalidate();
    }

    public void showTabList() {
        tabsProvider.enterTabMode(true);
    }

    public void openInNewBackgroundTabFromMenu(PageTitle title, HistoryEntry entry) {
        if (noPagesOpen()) {
            openInNewForegroundTabFromMenu(title, entry);
        } else {
            openInNewTabFromMenu(title, entry, getBackgroundTabPosition());
            tabsProvider.showAndHideTabs();
        }
    }

    public void openInNewForegroundTabFromMenu(PageTitle title, HistoryEntry entry) {
        openInNewTabFromMenu(title, entry, getForegroundTabPosition());
        loadPage(title, entry, PageLoadStrategy.Cache.FALLBACK, false);
    }

    public void openInNewTabFromMenu(PageTitle title,
                                     HistoryEntry entry,
                                     int position) {
        openInNewTab(title, entry, position);
        tabFunnel.logOpenInNew(tabList.size());
    }

    public void loadPage(PageTitle title, HistoryEntry entry, PageLoadStrategy.Cache cachePreference,
                         boolean pushBackStack) {
        loadPage(title, entry, cachePreference, pushBackStack, 0);
    }

    public void loadPage(PageTitle title, HistoryEntry entry, PageLoadStrategy.Cache cachePreference,
                         boolean pushBackStack, int stagedScrollY) {
        loadPage(title, entry, cachePreference, pushBackStack, stagedScrollY, false);
    }

    public void loadPage(PageTitle title, HistoryEntry entry, PageLoadStrategy.Cache cachePreference,
                         boolean pushBackStack, boolean pageRefreshed) {
        loadPage(title, entry, cachePreference, pushBackStack, 0, pageRefreshed);
    }

    /**
     * Load a new page into the WebView in this fragment.
     * This shall be the single point of entry for loading content into the WebView, whether it's
     * loading an entirely new page, refreshing the current page, retrying a failed network
     * request, etc.
     * @param title Title of the new page to load.
     * @param entry HistoryEntry associated with the new page.
     * @param cachePreference Whether to try loading the page from cache or from network.
     * @param pushBackStack Whether to push the new page onto the backstack.
     */
    public void loadPage(PageTitle title, HistoryEntry entry, PageLoadStrategy.Cache cachePreference,
                         boolean pushBackStack, int stagedScrollY, boolean pageRefreshed) {
        // disable sliding of the ToC while sections are loading
        tocHandler.setEnabled(false);

        errorState = false;
        errorView.setVisibility(View.GONE);

        model.setTitle(title);
        model.setTitleOriginal(title);
        model.setCurEntry(entry);

        updateProgressBar(true, true, 0);

        this.pageRefreshed = pageRefreshed;

        closePageScrollFunnel();
        pageLoadStrategy.load(pushBackStack, cachePreference, stagedScrollY);
        updateBookmark();
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

    // TODO: Update bookmark icon on the page action toolbar
    public void updateBookmark() {
        ReadingList.DAO.anyListContainsTitleAsync(ReadingListDaoProxy.key(getTitle()),
                new CallbackTask.Callback<ReadingListPage>() {
                    @Override public void success(@Nullable ReadingListPage page) {
                        if (!isAdded()) {
                            return;
                        }
                        if (page != null) {
                            pageActionTabsCallback.updateBookmark(true);
                            page.touch();
                            ReadingListPageDao.instance().upsert(page);
                            if (page.savedOrSaving()) {
                                // TODO: mark the page outdated only if the revision ID from the server
                                // is newer than the one on disk.
                                ReadingListPageDao.instance().markOutdated(page);
                            }
                        } else {
                            pageActionTabsCallback.updateBookmark(false);
                        }
                    }
                });
    }

    public void onActionModeShown(CompatActionMode mode) {
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
            pageLoadStrategy.backFromEditing(data);
            FeedbackUtil.showMessage(getActivity(), R.string.edit_saved_successfully);
            // and reload the page...
            loadPage(model.getTitleOriginal(), model.getCurEntry(), PageLoadStrategy.Cache.NONE, false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        if (!isSearching()) {
            inflater.inflate(R.menu.menu_page_actions, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || isSearching()
                || !(getHostTopFragment() instanceof PageFragment)) {
            return;
        }

        MenuItem otherLangItem = menu.findItem(R.id.menu_page_other_languages);
        MenuItem shareItem = menu.findItem(R.id.menu_page_share);
        MenuItem addToListItem = menu.findItem(R.id.menu_page_add_to_list);
        MenuItem findInPageItem = menu.findItem(R.id.menu_page_find_in_page);
        MenuItem contentIssues = menu.findItem(R.id.menu_page_content_issues);
        MenuItem similarTitles = menu.findItem(R.id.menu_page_similar_titles);
        MenuItem themeChooserItem = menu.findItem(R.id.menu_page_font_and_theme);
        MenuItem tabsItem = menu.findItem(R.id.menu_page_show_tabs);

        tabsItem.setIcon(ResourceUtil.getTabListIcon(getContext(), tabList.size()));

        if (pageLoadStrategy.isLoading() || errorState) {
            otherLangItem.setEnabled(false);
            shareItem.setEnabled(false);
            addToListItem.setEnabled(false);
            findInPageItem.setEnabled(false);
            contentIssues.setEnabled(false);
            similarTitles.setEnabled(false);
            themeChooserItem.setEnabled(false);
        } else {
            // Only display "Read in other languages" if the article is in other languages
            otherLangItem.setVisible(model.getPage() != null && model.getPage().getPageProperties().getLanguageCount() != 0);
            otherLangItem.setEnabled(true);
            shareItem.setEnabled(model.getPage() != null && model.getPage().isArticle());
            addToListItem.setEnabled(model.getPage() != null && model.getPage().isArticle());
            findInPageItem.setEnabled(true);
            updateMenuPageInfo(menu);
            themeChooserItem.setEnabled(true);
        }
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
                addToReadingList(AddToReadingListDialog.InvokeSource.PAGE_OVERFLOW_MENU);
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
            case R.id.menu_page_show_tabs:
                tabsProvider.enterTabMode(false);
                return true;
            case R.id.menu_page_search:
                if (callback() != null) {
                    callback().onPageSearchRequested();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sharePageLink() {
        if (getPage() != null) {
            ShareUtil.shareText(getActivity(), getPage().getTitle());
        }
    }

    public void showFindInPage() {
        if (model.getPage() == null) {
            return;
        }
        final FindInPageFunnel funnel = new FindInPageFunnel(app, model.getTitle().getSite(),
                model.getPage().getPageProperties().getPageId());
        final FindInPageActionProvider findInPageActionProvider
                = new FindInPageActionProvider(this, funnel);

        startSupportActionMode(new ActionMode.Callback() {
            private final String actionModeTag = "actionModeFindInPage";

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                findInPageActionMode = mode;
                MenuItem menuItem = menu.add(R.string.menu_page_find_in_page);
                MenuItemCompat.setActionProvider(menuItem, findInPageActionProvider);
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
    public void scrollToSection(String sectionAnchor) {
        if (!isAdded() || tocHandler == null) {
            return;
        }
        tocHandler.scrollToSection(sectionAnchor);
    }

    public void onPageLoadComplete() {
        refreshView.setEnabled(true);
        editHandler.setPage(model.getPage());
        initPageScrollFunnel();

        // TODO: update this title in the db to be queued for saving by the service.

        checkAndShowSelectTextOnboarding();

        if (getPageLoadCallbacks() != null) {
            getPageLoadCallbacks().onLoadComplete();
        }
    }

    public void onPageLoadError(Throwable caught) {
        if (!isAdded()) {
            return;
        }
        // in any case, make sure the TOC drawer is closed
        tocDrawer.closeDrawers();
        updateProgressBar(false, true, 0);
        refreshView.setRefreshing(false);

        if (pageRefreshed) {
            pageRefreshed = false;
            FeedbackUtil.showError(getActivity(), caught);
        }

        hidePageContent();
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
        if (getActivity() != null) {
            refreshView.setEnabled(ThrowableUtil.isRetryable(getActivity(), caught));
        }
        errorState = true;

        if (getPageLoadCallbacks() != null) {
            getPageLoadCallbacks().onLoadError(caught);
        }
    }

    /**
     * Read URL mappings from the saved page specific file
     */
    public void readUrlMappings() {
        new LoadSavedPageUrlMapTask(model.getTitle()) {
            @Override
            public void onFinish(JSONObject result) {
                // have we been unwittingly detached from our Activity?
                if (!isAdded()) {
                    L.d("Detached from activity, so stopping update.");
                    return;
                }

                ImageUrlMap.replaceImageSources(bridge, result);
            }

            @Override
            public void onCatch(Throwable e) {
                if (!isAdded()) {
                    return;
                }
                /*
                If anything bad happens during loading of a saved page, then simply bounce it
                back to the online version of the page, and re-save the page contents locally when it's done.
                 */
                L.d(e);
                refreshPage();
            }
        }.execute();
    }

    public void refreshPage() {
        if (pageLoadStrategy.isLoading()) {
            refreshView.setRefreshing(false);
            return;
        }

        errorView.setVisibility(View.GONE);
        errorState = false;

        model.setCurEntry(new HistoryEntry(model.getTitle(), HistoryEntry.SOURCE_HISTORY));
        loadPage(model.getTitle(), model.getCurEntry(), PageLoadStrategy.Cache.NONE, false, true);
    }

    private ToCHandler tocHandler;
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

    public void setupToC(PageViewModel model, boolean isFirstPage) {
        tocHandler.setupToC(model.getPage(), model.getTitle().getSite(), isFirstPage);
        tocHandler.setEnabled(true);
    }

    private void updateMenuPageInfo(@NonNull Menu menu) {
        MenuItem contentIssues = menu.findItem(R.id.menu_page_content_issues);
        MenuItem similarTitles = menu.findItem(R.id.menu_page_similar_titles);
        contentIssues.setVisible(pageInfo != null && pageInfo.hasContentIssues());
        contentIssues.setEnabled(true);
        similarTitles.setVisible(pageInfo != null && pageInfo.hasSimilarTitles());
        similarTitles.setEnabled(true);
    }

    private void setBookmarkIconForPageSavedState(boolean pageSaved) {
        TabLayout.Tab bookmarkTab
                = tabLayout.getTabAt(PageActionTab.ADD_TO_READING_LIST.code());
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

    private void openInNewTab(PageTitle title, HistoryEntry entry, int position) {
        if (shouldCreateNewTab()) {
            // create a new tab
            Tab tab = new Tab();
            // if the requested position is at the top, then make its backstack current
            if (position == getForegroundTabPosition()) {
                pageLoadStrategy.setBackStack(tab.getBackStack());
            }
            // put this tab in the requested position
            tabList.add(position, tab);
            trimTabCount();
            tabsProvider.invalidate();
            // add the requested page to its backstack
            tab.getBackStack().add(new PageBackStackItem(title, entry));
            getActivity().supportInvalidateOptionsMenu();
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
        bridge.addListener("ipaSpan", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
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
            }
        });
        bridge.addListener("imageClicked", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String href = decodeURL(messagePayload.getString("href"));
                    if (href.startsWith("/wiki/")) {
                        String filename = UriUtil.removeInternalLinkPrefix(href);
                        Site site = model.getTitle().getSite();
                        startActivityForResult(GalleryActivity.newIntent(getContext(),
                                model.getTitleOriginal(), filename, site,
                                GalleryFunnel.SOURCE_NON_LEAD_IMAGE),
                                Constants.ACTIVITY_REQUEST_GALLERY);
                    } else {
                        linkHandler.onUrlClick(href);
                    }
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
        bridge.addListener("mediaClicked", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String href = decodeURL(messagePayload.getString("href"));
                    String filename = StringUtil.removeUnderscores(UriUtil.removeInternalLinkPrefix(href));
                    Site site = model.getTitle().getSite();
                    startActivityForResult(GalleryActivity.newIntent(getContext(),
                            model.getTitleOriginal(), filename, site,
                            GalleryFunnel.SOURCE_NON_LEAD_IMAGE),
                            Constants.ACTIVITY_REQUEST_GALLERY);
                } catch (JSONException e) {
                    L.logRemoteErrorIfProd(e);
                }
            }
        });
    }

    /**
     * Convenience method for hiding all the content of a page.
     */
    private void hidePageContent() {
        leadImagesHandler.hide();
        toolbarHideHandler.setFadeEnabled(false);
        pageLoadStrategy.onHidePageContent();
        webView.setVisibility(View.INVISIBLE);
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
        if (pageLoadStrategy.popBackStack()) {
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

    public LinkHandler getLinkHandler() {
        return linkHandler;
    }

    public void updatePageInfo(@Nullable PageInfo pageInfo) {
        this.pageInfo = pageInfo;
        if (getActivity() != null) {
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    private void checkAndShowSelectTextOnboarding() {
        if (model.getPage().isArticle()
                &&  app.getOnboardingStateMachine().isSelectTextTutorialEnabled()) {
            showSelectTextOnboarding();
        }
    }

    private void showSelectTextOnboarding() {
        final View targetView = getView().findViewById(R.id.fragment_page_tool_tip_select_text_target);
        targetView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    ToolTipUtil.showToolTip(getActivity(),
                                            targetView, R.layout.inflate_tool_tip_select_text,
                                            ToolTip.Position.CENTER);
                    app.getOnboardingStateMachine().setSelectTextTutorial();
                }
            }
        }, TimeUnit.SECONDS.toMillis(1));
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

    private class PageFragmentLongPressHandler extends PageContainerLongPressHandler
            implements LongPressHandler.WebViewContextMenuListener {

        PageFragmentLongPressHandler(@NonNull PageFragment.Callback callback) {
            super(callback);
        }

        @Override
        public Site getSite() {
            return model.getTitleOriginal().getSite();
        }
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

    @Nullable
    public PageToolbarHideHandler getSearchBarHideHandler() {
        PageToolbarHideHandler handler = null;
        Callback callback = callback();
        if (callback != null) {
            handler = callback.onPageGetToolbarHideHandler();
        }
        return handler;
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

    private boolean isSearching() {
        boolean isSearching = false;
        Callback callback = callback();
        if (callback != null) {
            isSearching = callback.onPageIsSearching();
        }
        return isSearching;
    }

    @Nullable
    private Fragment getHostTopFragment() {
        Fragment fragment = null;
        Callback callback = callback();
        if (callback != null) {
            fragment = callback.onPageGetTopFragment();
        }
        return fragment;
    }

    private void showThemeChooser() {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageShowThemeChooser();
        }
    }

    @Nullable
    public ActionMode startSupportActionMode(@NonNull ActionMode.Callback actionModeCallback) {
        ActionMode actionMode = null;
        Callback callback = callback();
        if (callback != null) {
            actionMode = callback.onPageStartSupportActionMode(actionModeCallback);
        }
        return actionMode;
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

    @Nullable
    private PageLoadCallbacks getPageLoadCallbacks() {
        PageLoadCallbacks callbacks = null;
        Callback callback = callback();
        if (callback != null) {
            callbacks = callback.onPageGetPageLoadCallbacks();
        }
        return callbacks;
    }

    public void addToReadingList(@NonNull AddToReadingListDialog.InvokeSource source) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageAddToReadingList(getTitle(), source);
        }
    }

    @Nullable
    public View getContentView() {
        View view = null;
        Callback callback = callback();
        if (callback != null) {
            view = callback.onPageGetContentView();
        }
        return view;
    }

    @Nullable
    public View getTabsContainerView() {
        View view = null;
        Callback callback = callback();
        if (callback != null) {
            view = callback.onPageGetTabsContainerView();
        }
        return view;
    }

    private void startLangLinksActivity() {
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

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
