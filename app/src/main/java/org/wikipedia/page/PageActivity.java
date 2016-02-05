package org.wikipedia.page;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.IntentFunnel;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.snippet.CompatActionMode;
import org.wikipedia.random.RandomHandler;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.search.SearchArticlesFragment;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.theme.ThemeChooserDialog;
import org.wikipedia.tooltip.ToolTipUtil;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.WikiDrawerLayout;
import org.wikipedia.zero.WikipediaZeroHandler;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;
import org.wikipedia.zero.ZeroConfig;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import static org.wikipedia.util.DeviceUtil.isBackKeyUp;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class PageActivity extends ThemedActionBarActivity {

    public enum TabPosition {
        CURRENT_TAB,
        NEW_TAB_BACKGROUND,
        NEW_TAB_FOREGROUND
    }

    public static final int ACTIVITY_REQUEST_LANGLINKS = 0;
    public static final int ACTIVITY_REQUEST_EDIT_SECTION = 1;
    public static final int ACTIVITY_REQUEST_GALLERY = 2;

    public static final int PROGRESS_BAR_MAX_VALUE = 10000;

    public static final String ACTION_PAGE_FOR_TITLE = "org.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";
    public static final String EXTRA_SEARCH_FROM_WIDGET = "searchFromWidget";
    public static final String EXTRA_FEATURED_ARTICLE_FROM_WIDGET = "featuredArticleFromWidget";

    private static final String ZERO_ON_NOTICE_PRESENTED = "org.wikipedia.zero.zeroOnNoticePresented";
    private static final String LANGUAGE_CODE_BUNDLE_KEY = "language";
    private static final String PLAIN_TEXT_MIME_TYPE = "text/plain";
    private static final String LINK_PREVIEW_FRAGMENT_TAG = "link_preview_dialog";

    private Bus bus;
    private EventBusMethods busMethods;
    private WikipediaApp app;
    private View fragmentContainerView;
    private View tabsContainerView;
    private WikiDrawerLayout drawerLayout;
    private Menu navMenu;
    private SearchArticlesFragment searchFragment;
    private TextView searchHintText;
    private ProgressBar progressBar;
    private View toolbarContainer;
    private CompatActionMode currentActionMode;
    private ActionBarDrawerToggle mDrawerToggle;
    private SearchBarHideHandler searchBarHideHandler;
    private boolean isZeroEnabled;
    private ZeroConfig currentZeroConfig;
    private ThemeChooserDialog themeChooser;
    private RandomHandler randomHandler;
    private NavDrawerHelper navDrawerHelper;
    private boolean navItemSelected;

    public View getContentView() {
        return fragmentContainerView;
    }

    public View getTabsContainerView() {
        return tabsContainerView;
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return mDrawerToggle;
    }

    public SearchBarHideHandler getSearchBarHideHandler() {
        return searchBarHideHandler;
    }

    public Menu getNavMenu() {
        return navMenu;
    }

    public RandomHandler getRandomHandler() {
        return randomHandler;
    }

    /**
     * Get the Fragment that is currently at the top of the Activity's backstack.
     * This activity's fragment container will hold multiple fragments stacked onto
     * each other using FragmentManager, and this function will return the current
     * topmost Fragment. It's up to the caller to cast the result to a more specific
     * fragment class, and perform actions on it.
     * @return Fragment at the top of the backstack.
     */
    public Fragment getTopFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.content_fragment_container);
    }

    /**
     * Get the PageViewFragment that is currently at the top of the Activity's backstack.
     * If the current topmost fragment is not a PageViewFragment, return null.
     * @return The PageViewFragment at the top of the backstack, or null if the current
     * top fragment is not a PageViewFragment.
     */
    @Nullable public PageFragment getCurPageFragment() {
        Fragment f = getTopFragment();
        if (f instanceof PageFragment) {
            return (PageFragment) f;
        } else {
            return null;
        }
    }

    public void setNavItemSelected(boolean wasSelected) {
        navItemSelected = wasSelected;
    }

    private boolean wasNavItemSelected() {
        return navItemSelected;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getApplicationContext();
        app.checkCrashes(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_page);

        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbarContainer = findViewById(R.id.main_toolbar_container);

        busMethods = new EventBusMethods();
        registerBus();

        fragmentContainerView = findViewById(R.id.content_fragment_container);
        tabsContainerView = findViewById(R.id.tabs_container);
        progressBar = (ProgressBar)findViewById(R.id.main_progressbar);
        progressBar.setMax(PROGRESS_BAR_MAX_VALUE);
        updateProgressBar(false, true, 0);

        drawerLayout = (WikiDrawerLayout) findViewById(R.id.drawer_layout);
        if (!ApiUtil.hasLollipop()) {
            drawerLayout.setDrawerShadow(R.drawable.nav_drawer_shadow, GravityCompat.START);
        }
        NavigationView navDrawer = (NavigationView) findViewById(R.id.navdrawer);
        navMenu = navDrawer.getMenu();
        navDrawerHelper = new NavDrawerHelper(this, navDrawer.getHeaderView(0));
        navDrawer.setNavigationItemSelectedListener(navDrawerHelper.getNewListener());

        randomHandler = navDrawerHelper.getNewRandomHandler();

        searchFragment = (SearchArticlesFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);
        searchHintText = (TextView) findViewById(R.id.main_search_bar_text);
        searchHintText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchFragment.openSearch();
            }
        });

        mDrawerToggle = new MainDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,          /* DrawerLayout object */
                R.string.app_name,     /* "open drawer" description */
                R.string.app_name      /* "close drawer" description */
        );

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(mDrawerToggle);
        drawerLayout.setDragEdgeWidth(
                getResources().getDimensionPixelSize(R.dimen.drawer_drag_margin));
        getSupportActionBar().setTitle("");

        searchBarHideHandler = new SearchBarHideHandler(this, toolbarContainer);

        boolean languageChanged = false;
        if (savedInstanceState != null) {
            isZeroEnabled = savedInstanceState.getBoolean("pausedZeroEnabledState");
            currentZeroConfig = savedInstanceState.getParcelable("pausedZeroConfig");
            if (savedInstanceState.containsKey("themeChooserShowing")) {
                if (savedInstanceState.getBoolean("themeChooserShowing")) {
                    showThemeChooser();
                }
            }
            if (savedInstanceState.getBoolean("isSearching")) {
                searchFragment.openSearch();
            }
            String language = savedInstanceState.getString(LANGUAGE_CODE_BUNDLE_KEY);
            languageChanged = !app.getAppOrSystemLanguageCode().equals(language);

            // Note: when system language is enabled, and the system language is changed outside of
            // the app, MRU languages are not updated. There's no harm in doing that here but since
            // the user didin't choose that language in app, it may be unexpected.
        }
        searchHintText.setText(getString(isZeroEnabled ? R.string.zero_search_hint : R.string.search_hint));

        if (languageChanged) {
            app.resetSite();
            loadMainPageInForegroundTab();
        }

        if (savedInstanceState == null) {
            // if there's no savedInstanceState, and we're not coming back from a Theme change,
            // then we must have been launched with an Intent, so... handle it!
            handleIntent(getIntent());
        }

        // Conditionally execute all recurring tasks
        new RecurringTasksExecutor(app).run();
    }

    private void finishActionMode() {
        currentActionMode.finish();
    }

    private void nullifyActionMode() {
        currentActionMode = null;
    }

    private class MainDrawerToggle extends ActionBarDrawerToggle {
        private boolean oncePerSlideLock = false;

        MainDrawerToggle(android.app.Activity activity,
                         android.support.v4.widget.DrawerLayout drawerLayout,
                         int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, openDrawerContentDescRes, closeDrawerContentDescRes);
        }

        @Override
        public void onDrawerClosed(View view) {
            super.onDrawerClosed(view);
            // if we want to change the title upon closing:
            //getSupportActionBar().setTitle("");
            if (!wasNavItemSelected()) {
                navDrawerHelper.getFunnel().logCancel();
            }
            setNavItemSelected(false);
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            // if we want to change the title upon opening:
            //getSupportActionBar().setTitle("");
            // If we're in the search state, then get out of it.
            if (isSearching()) {
                searchFragment.closeSearch();
            }
            // also make sure we're not inside an action mode
            if (isCabOpen()) {
                finishActionMode();
            }
            updateNavDrawerSelection(getTopFragment());
            navDrawerHelper.getFunnel().logOpen();
        }

        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            super.onDrawerSlide(drawerView, 0);
            if (!oncePerSlideLock) {
                // Hide the keyboard when the drawer is opened
                hideSoftKeyboard(PageActivity.this);
                //also make sure ToC is hidden
                if (getCurPageFragment() != null) {
                    getCurPageFragment().toggleToC(PageFragment.TOC_ACTION_HIDE);
                }
                //and make sure to update dynamic items and highlights
                navDrawerHelper.setupDynamicNavDrawerItems();
                oncePerSlideLock = true;
            }
            // and make sure the Toolbar is showing
            showToolbar();
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            super.onDrawerStateChanged(newState);
            if (newState == DrawerLayout.STATE_IDLE) {
                oncePerSlideLock = false;
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    // Note: this method is invoked even when in CAB mode.
    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return isBackKeyUp(event) && ToolTipUtil.dismissToolTip(this)
                || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle other action bar items...
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSearchRequested() {
        showToolbar();
        searchFragment.openSearch();
        return true;
    }

    public void showToolbar() {
        ViewAnimations.ensureTranslationY(toolbarContainer, 0);
    }

    public void setNavMenuItemRandomEnabled(boolean enabled) {
        navMenu.findItem(R.id.nav_item_random).setEnabled(enabled);
    }

    /** @return True if the contextual action bar is open. */
    public boolean isCabOpen() {
        return currentActionMode != null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Site site = new Site(intent.getData().getAuthority());
            PageTitle title = site.titleForUri(intent.getData());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);
            loadPageInForegroundTab(title, historyEntry);
        } else if (ACTION_PAGE_FOR_TITLE.equals(intent.getAction())) {
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
            loadPage(title, historyEntry);
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            PageTitle title = new PageTitle(query, app.getSite());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
            loadPageInForegroundTab(title, historyEntry);
        } else if (Intent.ACTION_SEND.equals(intent.getAction())
                && PLAIN_TEXT_MIME_TYPE.equals(intent.getType())) {
            new IntentFunnel(app).logShareIntent();
            handleShareIntent(intent);
        } else if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction())
                && PLAIN_TEXT_MIME_TYPE.equals(intent.getType())) {
            new IntentFunnel(app).logProcessTextIntent();
            handleProcessTextIntent(intent);
        } else if (intent.hasExtra(EXTRA_SEARCH_FROM_WIDGET)) {
            new IntentFunnel(app).logSearchWidgetTap();
            openSearch();
        } else if (intent.hasExtra(EXTRA_FEATURED_ARTICLE_FROM_WIDGET)) {
            new IntentFunnel(app).logFeaturedArticleWidgetTap();
            loadMainPageInForegroundTab();
        } else {
            loadMainPageIfNoTabs();
        }
    }

    private void handleShareIntent(Intent intent) {
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        openSearch(text == null ? null : text.trim());
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleProcessTextIntent(Intent intent) {
        if (!ApiUtil.hasMarshmallow()) {
            return;
        }
        String text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
        openSearch(text == null ? null : text.trim());
    }

    private void openSearch() {
        openSearch(null);
    }

    private void openSearch(@Nullable final CharSequence query) {
        fragmentContainerView.post(new Runnable() {
            @Override
            public void run() {
                searchFragment.setLaunchedFromWidget(true);
                searchFragment.openSearch();
                if (query != null) {
                    searchFragment.setSearchText(query);
                }
            }
        });
    }

    /**
     * Update the state of the main progress bar that is shown inside the ActionBar of the activity.
     * @param visible Whether the progress bar is visible.
     * @param indeterminate Whether the progress bar is indeterminate.
     * @param value Value of the progress bar (may be between 0 and 10000). Ignored if the
     *              progress bar is indeterminate.
     */
    public void updateProgressBar(boolean visible, boolean indeterminate, int value) {
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            progressBar.setProgress(value);
        }
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns whether we're currently in a "searching" state (i.e. the search fragment is shown).
     * @return True if currently searching, false otherwise.
     */
    public boolean isSearching() {
        return searchFragment != null && searchFragment.isSearchActive();
    }

    public void closeNavDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void removeAllFragments() {
        getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    /**
     * Add a new fragment to the top of the activity's backstack.
     * @param f New fragment to place on top.
     */
    public void pushFragment(Fragment f) {
        pushFragment(f, false);
    }

    /**
     * Add a new fragment to the top of the activity's backstack, and optionally allow state loss.
     * Useful for cases where we might push a fragment from an AsyncTask result.
     * @param f New fragment to place on top.
     * @param allowStateLoss Whether to allow state loss.
     */
    public void pushFragment(Fragment f, boolean allowStateLoss) {
        closeNavDrawer();
        searchBarHideHandler.setForceNoFade(false);
        searchBarHideHandler.setFadeEnabled(false);
        // if the new fragment is the same class as the current topmost fragment,
        // then just keep the previous fragment there.
        // e.g. if the user selected History, and there's already a History fragment on top,
        // then there's no need to load a new History fragment.
        if (getTopFragment() != null && (getTopFragment().getClass() == f.getClass())) {
            return;
        }

        removeAllFragments();
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        trans.add(R.id.content_fragment_container, f);
        trans.addToBackStack(null);
        if (allowStateLoss) {
            trans.commitAllowingStateLoss();
        } else {
            trans.commit();
        }

        // and make sure the ActionBar is visible
        showToolbar();
        //also make sure the progress bar is not showing
        updateProgressBar(false, true, 0);
    }

    public void resetAfterClearHistory() {
        removeAllFragments();
        Prefs.clearTabs();
        loadMainPageIfNoTabs();
    }

    /**
     * Load a new page, and put it on top of the backstack.
     * @param title Title of the page to load.
     * @param entry HistoryEntry associated with this page.
     */
    public void loadPage(PageTitle title, HistoryEntry entry) {
        loadPage(title, entry, TabPosition.CURRENT_TAB, false);
    }

    public void loadPage(PageTitle title,
                         HistoryEntry entry,
                         TabPosition position,
                         boolean allowStateLoss) {
        loadPage(title, entry, position, allowStateLoss, false);
    }

    /**
     * Load a new page, and put it on top of the backstack, optionally allowing state loss of the
     * fragment manager. Useful for when this function is called from an AsyncTask result.
     * @param title Title of the page to load.
     * @param entry HistoryEntry associated with this page.
     * @param position Whether to open this page in the current tab, a new background tab, or new
     *                 foreground tab.
     * @param allowStateLoss Whether to allow state loss.
     * @param mustBeEmpty If true, and a tab exists already, do nothing.
     */
    public void loadPage(final PageTitle title,
                         final HistoryEntry entry,
                         final TabPosition position,
                         boolean allowStateLoss,
                         final boolean mustBeEmpty) {
        if (isDestroyed()) {
            return;
        }

        // Close the link preview, if one is open.
        hideLinkPreview();

        app.putCrashReportProperty("api", title.getSite().getDomain());
        app.putCrashReportProperty("title", title.toString());

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeNavDrawer();
        }
        if (title.isSpecial()) {
            visitInExternalBrowser(this, Uri.parse(title.getMobileUri()));
            return;
        }

        pushFragment(new PageFragment(), allowStateLoss);

        fragmentContainerView.post(new Runnable() {
            @Override
            public void run() {
                PageFragment frag = getCurPageFragment();
                if (frag == null) {
                    return;
                }
                //is the new title the same as what's already being displayed?
                if (position == TabPosition.CURRENT_TAB
                        && !frag.getCurrentTab().getBackStack().isEmpty()
                        && frag.getCurrentTab().getBackStack()
                        .get(frag.getCurrentTab().getBackStack().size() - 1).getTitle()
                        .equals(title) || mustBeEmpty && !frag.getCurrentTab().getBackStack().isEmpty()) {
                    //if we have a section to scroll to, then pass it to the fragment
                    if (!TextUtils.isEmpty(title.getFragment())) {
                        frag.scrollToSection(title.getFragment());
                    }
                    return;
                }
                frag.closeFindInPage();
                if (position == TabPosition.CURRENT_TAB) {
                    frag.loadPage(title, entry, PageLoadStrategy.Cache.FALLBACK, true);
                } else if (position == TabPosition.NEW_TAB_BACKGROUND) {
                    frag.openInNewBackgroundTabFromMenu(title, entry);
                } else {
                    frag.openInNewForegroundTabFromMenu(title, entry);
                }
                app.getSessionFunnel().pageViewed(entry);
            }
        });
    }

    public void loadPageInForegroundTab(PageTitle title, HistoryEntry entry) {
        loadPage(title, entry, TabPosition.NEW_TAB_FOREGROUND, false);
    }

    public void loadMainPageInCurrentTab() {
        loadMainPage(false, TabPosition.CURRENT_TAB, false);
    }

    public void loadMainPageInForegroundTab() {
        loadMainPage(true, TabPosition.NEW_TAB_FOREGROUND, false);
    }

    /**
     * Go directly to the Main Page of the current Wiki, optionally allowing state loss of the
     * fragment manager. Useful for when this function is called from an AsyncTask result.
     * @param allowStateLoss Allows the {@link android.support.v4.app.FragmentManager} commit to be
     *                       executed after an activity's state is saved.  This is dangerous because
     *                       the commit can be lost if the activity needs to later be restored from
     *                       its state, so this should only be used for cases where it is okay for
     *                       the UI state to change unexpectedly on the user.
     * @param mustBeEmpty If true, and a tab exists already, do nothing.
     */
    public void loadMainPage(boolean allowStateLoss, TabPosition position, boolean mustBeEmpty) {
        PageTitle title = new PageTitle(MainPageNameData.valueFor(app.getAppOrSystemLanguageCode()), app.getSite());
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE);
        loadPage(title, historyEntry, position, allowStateLoss, mustBeEmpty);
    }

    public void showLinkPreview(PageTitle title, int entrySource) {
        showLinkPreview(title, entrySource, null);
    }

    public void showLinkPreview(PageTitle title, int entrySource, @Nullable Location location) {
        if (getSupportFragmentManager().findFragmentByTag(LINK_PREVIEW_FRAGMENT_TAG) == null) {
            DialogFragment linkPreview = LinkPreviewDialog.newInstance(title, entrySource, location);
            linkPreview.show(getSupportFragmentManager(), LINK_PREVIEW_FRAGMENT_TAG);
        }
    }

    /**
     * Dismiss the current link preview, if one is open.
     */
    private void hideLinkPreview() {
        DialogFragment linkPreview = (DialogFragment) getSupportFragmentManager().findFragmentByTag(LINK_PREVIEW_FRAGMENT_TAG);
        if (linkPreview != null) {
            linkPreview.dismiss();
        }
    }

    public void showThemeChooser() {
        if (themeChooser == null) {
            themeChooser = new ThemeChooserDialog(this);
        }
        themeChooser.show();
    }

    // Note: back button first handled in {@link #onOptionsItemSelected()};
    @Override
    public void onBackPressed() {
        if (ToolTipUtil.dismissToolTip(this)) {
            return;
        }
        if (isCabOpen()) {
            finishActionMode();
            return;
        }
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeNavDrawer();
            return;
        }
        if (searchFragment.onBackPressed()) {
            if (searchFragment.isLaunchedFromWidget()) {
                finish();
            }
            return;
        }
        app.getSessionFunnel().backPressed();
        if (getTopFragment() instanceof BackPressedHandler
                && ((BackPressedHandler) getTopFragment()).onBackPressed()) {
            return;
        } else if (!(getTopFragment() instanceof PageFragment)) {
            pushFragment(new PageFragment(), false);
            return;
        }
        finish();
    }

    /*package*/ void showPageSavedMessage(@NonNull String title, boolean success) {
        FeedbackUtil.showMessage(this, getString(success
                ? R.string.snackbar_saved_page_format
                : R.string.snackbar_saved_page_missing_images, title));
    }

    private void loadMainPageIfNoTabs() {
        loadMainPage(false, TabPosition.CURRENT_TAB, true);
    }

    private class EventBusMethods {
        @Subscribe
        public void onChangeTextSize(ChangeTextSizeEvent event) {
            if (getCurPageFragment() != null && getCurPageFragment().getWebView() != null) {
                getCurPageFragment().updateFontSize();
            }
        }

        @Subscribe
        public void onChangeTheme(ThemeChangeEvent event) {
            PageActivity.this.recreate();
        }

        @Subscribe
        public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
            boolean latestWikipediaZeroDisposition = app.getWikipediaZeroHandler().isZeroEnabled();
            ZeroConfig latestZeroConfig = app.getWikipediaZeroHandler().getZeroConfig();

            if (isZeroEnabled && !latestWikipediaZeroDisposition) {
                String title = getString(R.string.zero_charged_verbiage);
                String verbiage = getString(R.string.zero_charged_verbiage_extended);
                WikipediaZeroHandler.showZeroBanner(PageActivity.this, title,
                        getResources().getColor(android.R.color.white),
                        getResources().getColor(R.color.holo_red_dark));
                navDrawerHelper.setupDynamicNavDrawerItems();
                showDialogAboutZero(null, title, verbiage);
            } else if ((!isZeroEnabled || !currentZeroConfig.equals(latestZeroConfig))
                       && latestWikipediaZeroDisposition) {
                String title = latestZeroConfig.getMessage();
                String verbiage = getString(R.string.zero_learn_more);
                WikipediaZeroHandler.showZeroBanner(PageActivity.this, title,
                        latestZeroConfig.getForeground(), latestZeroConfig.getBackground());
                navDrawerHelper.setupDynamicNavDrawerItems();
                showDialogAboutZero(ZERO_ON_NOTICE_PRESENTED, title, verbiage);
            }
            isZeroEnabled = latestWikipediaZeroDisposition;
            currentZeroConfig = latestZeroConfig;
            searchHintText.setText(getString(
                    latestWikipediaZeroDisposition
                            ? R.string.zero_search_hint
                            : R.string.search_hint));
        }
    }

    private void showDialogAboutZero(final String prefsKey, String title, String message) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
        if (prefsKey == null || !prefs.getBoolean(prefsKey, false)) {
            if (prefsKey != null) {
                prefs.edit().putBoolean(prefsKey, true).apply();
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage(Html.fromHtml("<b>" + title + "</b><br/><br/>" + message));
            if (prefsKey != null) {
                alert.setPositiveButton(getString(R.string.zero_learn_more_learn_more), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        visitInExternalBrowser(PageActivity.this, Uri.parse(getString(R.string.zero_webpage_url)));
                    }
                });
            }
            alert.setNegativeButton(getString(R.string.zero_learn_more_dismiss), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });
            AlertDialog ad = alert.create();
            ad.show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (bus == null) {
            registerBus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.resetSite();
        app.getSessionFunnel().touchSession();
        boolean latestWikipediaZeroDisposition = app.getWikipediaZeroHandler().isZeroEnabled();
        if (isZeroEnabled && !latestWikipediaZeroDisposition) {
            bus.post(new WikipediaZeroStateChangeEvent());
        }
        navDrawerHelper.setupDynamicNavDrawerItems();
    }

    @Override
    public void onPause() {
        super.onPause();
        isZeroEnabled = app.getWikipediaZeroHandler().isZeroEnabled();
        currentZeroConfig = app.getWikipediaZeroHandler().getZeroConfig();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    public void updateNavDrawerSelection(Fragment fragment) {
        navDrawerHelper.updateItemSelection(fragment);
    }

    private void saveState(Bundle outState) {
        outState.putBoolean("pausedZeroEnabledState", isZeroEnabled);
        outState.putParcelable("pausedZeroConfig", currentZeroConfig);
        if (themeChooser != null) {
            outState.putBoolean("themeChooserShowing", themeChooser.isShowing());
        }
        outState.putBoolean("isSearching", isSearching());
        outState.putString(LANGUAGE_CODE_BUNDLE_KEY, app.getAppOrSystemLanguageCode());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (bus == null) {
            registerBus();
        }
        if (settingsActivityRequested(requestCode)) {
            handleSettingsActivityResult(resultCode);
        } else if (loginActivityRequested(requestCode)) {
            handleLoginActivityResult(resultCode);
        } else if (newArticleLanguageSelected(requestCode, resultCode) || galleryFilePageSelected(requestCode, resultCode)) {
            handleLangLinkOrFilePageResult(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleLangLinkOrFilePageResult(final Intent data) {
        fragmentContainerView.post(new Runnable() {
            @Override
            public void run() {
                handleIntent(data);
            }
        });
    }

    @Override
    protected void onStop() {
        if (themeChooser != null && themeChooser.isShowing()) {
            themeChooser.dismiss();
        }
        app.getSessionFunnel().persistSession();

        super.onStop();
        unregisterBus();
    }

    /**
     * ActionMode that is invoked when the user long-presses inside the WebView.
     * @param mode ActionMode under which this context is starting.
     */
    @Override
    public void onSupportActionModeStarted(ActionMode mode) {
        if (!isCabOpen()) {
            conditionallyInjectCustomCabMenu(mode);
        }
        freezeToolbar();
        super.onSupportActionModeStarted(mode);
    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        nullifyActionMode();
        searchBarHideHandler.setForceNoFade(false);
    }

    @Override
    public void onActionModeStarted(android.view.ActionMode mode) {
        if (!isCabOpen()) {
            conditionallyInjectCustomCabMenu(mode);
        }
        freezeToolbar();
        super.onActionModeStarted(mode);
    }

    @Override
    public void onActionModeFinished(android.view.ActionMode mode) {
        super.onActionModeFinished(mode);
        nullifyActionMode();
        searchBarHideHandler.setForceNoFade(false);
    }

    private <T> void conditionallyInjectCustomCabMenu(T mode) {
        currentActionMode = new CompatActionMode(mode);
        if (currentActionMode.shouldInjectCustomMenu(PageActivity.this)) {
            currentActionMode.injectCustomMenu(PageActivity.this);
        }
    }

    private void freezeToolbar() {
        getSearchBarHideHandler().setForceNoFade(true);
    }

    private void registerBus() {
        bus = app.getBus();
        bus.register(busMethods);
        L.d("Registered bus.");
    }

    private void unregisterBus() {
        bus.unregister(busMethods);
        bus = null;
        L.d("Unregistered bus.");
    }

    private void handleSettingsActivityResult(int resultCode) {
        if (languageChanged(resultCode)) {
            loadNewLanguageMainPage();
        }
    }

    private boolean settingsActivityRequested(int requestCode) {
        return requestCode == SettingsActivity.ACTIVITY_REQUEST_SHOW_SETTINGS;
    }

    private void handleLoginActivityResult(int resultCode) {
        if (resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            FeedbackUtil.showMessage(this, R.string.login_success_toast);
        }
    }

    private boolean loginActivityRequested(int requestCode) {
        return requestCode == LoginActivity.REQUEST_LOGIN;
    }

    private boolean newArticleLanguageSelected(int requestCode, int resultCode) {
        return requestCode == ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT;
    }

    private boolean galleryFilePageSelected(int requestCode, int resultCode) {
        return requestCode == ACTIVITY_REQUEST_GALLERY && resultCode == GalleryActivity.ACTIVITY_RESULT_FILEPAGE_SELECT;
    }

    private boolean languageChanged(int resultCode) {
        return resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED;
    }

    /**
     * Reload the main page in the new language, after delaying for one second in order to:
     * (1) Make sure that onStart in PageActivity gets called, thus registering the activity for the bus.
     * (2) Ensure a smooth transition, which is very jarring without a delay.
     */
    private void loadNewLanguageMainPage() {
        Handler uiThread = new Handler(Looper.getMainLooper());
        uiThread.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadMainPageInForegroundTab();
                updateFeaturedPageWidget();
            }
        }, DateUtils.SECOND_IN_MILLIS);
    }

    /**
     * Update any instances of our Featured Page widget, since it will change with the currently selected language.
     */
    private void updateFeaturedPageWidget() {
        Intent widgetIntent = new Intent(this, WidgetProviderFeaturedPage.class);
        widgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(
                new ComponentName(this, WidgetProviderFeaturedPage.class));
        widgetIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(widgetIntent);
    }
}
