package org.wikipedia;

import android.annotation.TargetApi;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.hockeyapp.android.metrics.MetricsManager;

import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.analytics.IntentFunnel;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.analytics.WikipediaZeroUsageFunnel;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.events.WikipediaZeroStateChangeEvent;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.news.NewsActivity;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.NavDrawerHelper;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageLoadStrategy;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.gallery.ImagePipelineBitmapGetter;
import org.wikipedia.page.gallery.MediaDownloadReceiver;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.snippet.CompatActionMode;
import org.wikipedia.random.RandomHandler;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.ReadingListsFragment;
import org.wikipedia.recurring.RecurringTasksExecutor;
import org.wikipedia.search.SearchArticlesFragment;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.theme.ThemeChooserDialog;
import org.wikipedia.tooltip.ToolTipUtil;
import org.wikipedia.useroption.sync.UserOptionContentResolver;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.WikiDrawerLayout;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;
import org.wikipedia.zero.ZeroConfig;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.DeviceUtil.isBackKeyUp;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;
import static org.wikipedia.util.PermissionUtil.hasWriteExternalStoragePermission;
import static org.wikipedia.util.PermissionUtil.requestWriteStorageRuntimePermissions;

public class MainActivity extends ThemedActionBarActivity implements FeedFragment.Callback {

    public enum TabPosition {
        CURRENT_TAB,
        NEW_TAB_BACKGROUND,
        NEW_TAB_FOREGROUND
    }

    public static final int ACTIVITY_REQUEST_LANGLINKS = 0;
    public static final int ACTIVITY_REQUEST_EDIT_SECTION = 1;
    public static final int ACTIVITY_REQUEST_GALLERY = 2;
    public static final int ACTIVITY_REQUEST_VOICE_SEARCH = 3;

    public static final int PROGRESS_BAR_MAX_VALUE = 10000;

    public static final String ACTION_PAGE_FOR_TITLE = "org.wikipedia.page_for_title";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";
    public static final String EXTRA_SEARCH_FROM_WIDGET = "searchFromWidget";
    public static final String EXTRA_FEATURED_ARTICLE_FROM_WIDGET = "featuredArticleFromWidget";

    private static final String LANGUAGE_CODE_BUNDLE_KEY = "language";
    private static final String PLAIN_TEXT_MIME_TYPE = "text/plain";

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
    private RandomHandler randomHandler;
    private NavDrawerHelper navDrawerHelper;
    private boolean navItemSelected;
    private WikipediaZeroUsageFunnel zeroFunnel;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter(this);
    private MainActivityToolbarCoordinator toolbarCoordinator;

    // The permissions request API doesn't take a callback, so in the event we have to
    // ask for permission to download a featured image from the feed, we'll have to hold
    // the image we're waiting for permission to download as a bit of state here. :(
    private FeaturedImage pendingDownloadImage;

    private DialogInterface.OnDismissListener listDialogDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            if (getCurPageFragment() != null) {
                getCurPageFragment().updateBookmark();
            }
        }
    };

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
        MetricsManager.register(this, app);
        app.checkCrashes(this);

        if (ApiUtil.hasKitKat()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        toolbarContainer = findViewById(R.id.main_toolbar_container);
        toolbarCoordinator = new MainActivityToolbarCoordinator(this, toolbarContainer, (Toolbar) findViewById(R.id.main_toolbar));
        getSupportFragmentManager()
                .addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        updateToolbarForFragment();
                    }
                });

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
                searchFragment.setInvokeSource(SearchArticlesFragment.InvokeSource.TOOLBAR);
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

        zeroFunnel = app.getWikipediaZeroHandler().getZeroFunnel();
        if (savedInstanceState != null) {
            isZeroEnabled = savedInstanceState.getBoolean("pausedZeroEnabledState");
            currentZeroConfig = savedInstanceState.getParcelable("pausedZeroConfig");
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

        UserOptionContentResolver.requestManualSync();
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
            navDrawerHelper.clearTempExplicitHighlight();
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
                hideSoftKeyboard(MainActivity.this);
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

    public void setSearchMode(boolean enabled) {
        // invalidate our ActionBar, so that all action items are removed, and
        // we can fill up the whole width of the ActionBar with our SearchView.
        supportInvalidateOptionsMenu();
        toolbarCoordinator.setSearchMode(enabled);
        getSearchBarHideHandler().setForceNoFade(enabled);
        getDrawerToggle().setDrawerIndicatorEnabled(!enabled);
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

    @NonNull
    public static Intent newIntent(@NonNull Context context,
                                   @NonNull HistoryEntry entry,
                                   @NonNull PageTitle title) {
        return new Intent(MainActivity.ACTION_PAGE_FOR_TITLE)
                .setClass(context, MainActivity.class)
                .putExtra(MainActivity.EXTRA_HISTORYENTRY, entry)
                .putExtra(MainActivity.EXTRA_PAGETITLE, title);
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
            openSearchFromIntent(null, SearchArticlesFragment.InvokeSource.WIDGET);
        } else if (intent.hasExtra(EXTRA_FEATURED_ARTICLE_FROM_WIDGET)) {
            new IntentFunnel(app).logFeaturedArticleWidgetTap();
            loadMainPageInForegroundTab();
        } else if (TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.pageLastShown()) == 0) {
            loadMainPageIfNoTabs();
        } else {
            showFeed();
        }
    }

    private void handleShareIntent(Intent intent) {
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        openSearchFromIntent(text == null ? null : text.trim(),
                SearchArticlesFragment.InvokeSource.INTENT_SHARE);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void handleProcessTextIntent(Intent intent) {
        if (!ApiUtil.hasMarshmallow()) {
            return;
        }
        String text = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
        openSearchFromIntent(text == null ? null : text.trim(),
                SearchArticlesFragment.InvokeSource.INTENT_PROCESS_TEXT);
    }

    private void openSearchFromIntent(@Nullable final CharSequence query,
                                      final SearchArticlesFragment.InvokeSource source) {
        fragmentContainerView.post(new Runnable() {
            @Override
            public void run() {
                searchFragment.setInvokeSource(source);
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

    public void showFeed() {
        // pop fragments until we see a FeedFragment. If there's no FeedFragment, then add it.
        while (getSupportFragmentManager().getBackStackEntryCount() > 0
                && !(getTopFragment() instanceof FeedFragment)) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        pushFragment(new FeedFragment());
    }

    private void resetFragmentsToFeedOrPage() {
        while (getSupportFragmentManager().getBackStackEntryCount() > 0
                && !(getTopFragment() instanceof FeedFragment)
                && !(getTopFragment() instanceof PageFragment)) {
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    /**
     * Add a new fragment to the top of the activity's backstack.
     * @param f New fragment to place on top.
     */
    public void pushFragment(Fragment f) {
        pushFragment(f, false);
    }

    /**
     * Add a new fragment to the top of the activity's backstack, and optionally  allow state loss.
     * Useful for cases where we might push a fragment from an AsyncTask result.
     * @param f New fragment to place on top.
     * @param allowStateLoss Whether to allow state loss.
     */
    public void pushFragment(Fragment f, boolean allowStateLoss) {
        beforeFragmentChanged();
        // if the new fragment is the same class as the current topmost fragment,
        // then just keep the previous fragment there.
        // e.g. if the user selected History, and there's already a History fragment on top,
        // then there's no need to load a new History fragment.
        if (getTopFragment() != null && (getTopFragment().getClass() == f.getClass())) {
            return;
        }

        resetFragmentsToFeedOrPage();
        if (getTopFragment() == null || (getTopFragment().getClass() != f.getClass())) {
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            trans.setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out);
            trans.add(R.id.content_fragment_container, f);
            trans.addToBackStack(null);
            if (allowStateLoss) {
                trans.commitAllowingStateLoss();
            } else {
                trans.commit();
            }
        }
        afterFragmentChanged();
    }

    public void resetAfterClearHistory() {
        Prefs.clearTabs();
        showFeed();
    }

    private void beforeFragmentChanged() {
        closeNavDrawer();
        searchBarHideHandler.setForceNoFade(false);
        searchBarHideHandler.setFadeEnabled(false);
    }

    private void afterFragmentChanged() {
        //make sure the ActionBar is visible
        showToolbar();
        //also make sure the progress bar is not showing
        updateProgressBar(false, true, 0);
    }

    private void updateToolbarForFragment() {
        if (getTopFragment() instanceof MainActivityToolbarProvider) {
            toolbarCoordinator.setOverrideToolbar(((MainActivityToolbarProvider) getTopFragment()).getToolbar());
        } else {
            toolbarCoordinator.removeOverrideToolbar();
        }
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

        if (entry.getSource() != HistoryEntry.SOURCE_INTERNAL_LINK || !app.isLinkPreviewEnabled()) {
            new LinkPreviewFunnel(app, entry.getSource()).logNavigate();
        }

        // Close the link preview, if one is open.
        hideLinkPreview();

        app.putCrashReportProperty("api", title.getSite().authority());
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
        bottomSheetPresenter.show(LinkPreviewDialog.newInstance(title, entrySource, location));
    }

    private void hideLinkPreview() {
        bottomSheetPresenter.dismiss();
    }

    public void showThemeChooser() {
        bottomSheetPresenter.show(new ThemeChooserDialog());
    }

    public void dismissBottomSheet() {
        bottomSheetPresenter.dismiss();
    }

    public void showBottomSheet(BottomSheetDialog dialog) {
        bottomSheetPresenter.show(dialog);
    }

    public void showBottomSheet(BottomSheetDialogFragment dialog) {
        bottomSheetPresenter.show(dialog);
    }

    public void showAddToListDialog(PageTitle title, AddToReadingListDialog.InvokeSource source) {
        FeedbackUtil.showAddToListDialog(title, source, bottomSheetPresenter, listDialogDismissListener);
    }

    public void showReadingListAddedSnackbar(String message, final boolean isOnboarding) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(fragmentContainerView, message,
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.reading_list_added_view_button, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnboarding) {
                    navDrawerHelper.setTempExplicitHighlight(ReadingListsFragment.class);
                    drawerLayout.openDrawer(GravityCompat.START);
                } else {
                    pushFragment(new ReadingListsFragment());
                }
            }
        });
        snackbar.show();
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
            if (searchFragment.isLaunchedFromIntent()) {
                finish();
            }
            return;
        }
        app.getSessionFunnel().backPressed();
        if (getTopFragment() instanceof BackPressedHandler
                && ((BackPressedHandler) getTopFragment()).onBackPressed()) {
            return;
        }
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            beforeFragmentChanged();
            getSupportFragmentManager().popBackStackImmediate();
            afterFragmentChanged();
            return;
        }
        finish();
    }

    @Override
    public void onFeedSearchRequested() {
        searchFragment.setInvokeSource(SearchArticlesFragment.InvokeSource.FEED_BAR);
        onSearchRequested();
    }

    @Override
    public void onFeedVoiceSearchRequested() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        try {
            startActivityForResult(intent, ACTIVITY_REQUEST_VOICE_SEARCH);
        } catch (ActivityNotFoundException a) {
            FeedbackUtil.showMessage(this, R.string.error_voice_search_not_available);
        }
    }

    @Override
    public void onFeedSelectPage(HistoryEntry entry) {
        loadPage(entry.getTitle(), entry);
    }

    @Override
    public void onFeedAddPageToList(HistoryEntry entry) {
        showAddToListDialog(entry.getTitle(), AddToReadingListDialog.InvokeSource.FEED);
    }

    @Override
    public void onFeedSharePage(HistoryEntry entry) {
        ShareUtil.shareText(this, entry.getTitle());
    }

    @Override
    public void onFeedNewsItemSelected(NewsItemCard card) {
        startActivity(NewsActivity.newIntent(app, card.item(), card.site()));
    }

    @Override
    public void onFeedShareImage(final FeaturedImageCard card) {
        final String thumbUrl = card.baseImage().thumbnail().source().toString();
        final String fullSizeUrl = card.baseImage().image().source().toString();
        new ImagePipelineBitmapGetter(this, thumbUrl) {
            @Override
            public void onSuccess(@Nullable Bitmap bitmap) {
                if (bitmap != null) {
                    ShareUtil.shareImage(MainActivity.this,
                            bitmap,
                            new File(thumbUrl).getName(),
                            getString(R.string.feed_featured_image_share_subject) + " | "
                                    + DateUtil.getFeedCardDateString(card.date().baseCalendar()),
                            fullSizeUrl);
                } else {
                    FeedbackUtil.showMessage(MainActivity.this, getString(R.string.gallery_share_error, card.baseImage().title()));
                }
            }
        }.get();
    }

    @Override
    public void onFeedDownloadImage(@NonNull FeaturedImage image) {
        if (!(hasWriteExternalStoragePermission(this))) {
            setPendingDownload(image);
            requestWriteExternalStoragePermission();
        } else {
            download(image);
        }
    }

    private void download(@NonNull FeaturedImage image) {
        new MediaDownloadReceiver(MainActivity.this).download(image);
    }

    private void requestWriteExternalStoragePermission() {
        requestWriteStorageRuntimePermissions(this,
                Constants.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
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
            MainActivity.this.recreate();
        }

        @Subscribe
        public void onWikipediaZeroStateChangeEvent(WikipediaZeroStateChangeEvent event) {
            boolean latestZeroEnabledState = app.getWikipediaZeroHandler().isZeroEnabled();
            ZeroConfig latestZeroConfig = app.getWikipediaZeroHandler().getZeroConfig();

            if (leftZeroRatedNetwork(latestZeroEnabledState)) {
                app.getWikipediaZeroHandler().showZeroOffBanner(MainActivity.this,
                        getString(R.string.zero_charged_verbiage),
                        ContextCompat.getColor(MainActivity.this, R.color.holo_red_dark),
                        ContextCompat.getColor(MainActivity.this, android.R.color.white));
                navDrawerHelper.setupDynamicNavDrawerItems();
            }

            if (enteredNewZeroRatedNetwork(latestZeroConfig, latestZeroEnabledState)) {
                app.getWikipediaZeroHandler().showZeroBanner(MainActivity.this, latestZeroConfig);
                if (Prefs.isShowZeroInfoDialogEnabled()) {
                    showZeroInfoDialog(latestZeroConfig);
                    Prefs.setShowZeroInfoDialogEnable(false);
                }
                navDrawerHelper.setupDynamicNavDrawerItems();
            }

            isZeroEnabled = latestZeroEnabledState;
            currentZeroConfig = latestZeroConfig;
            searchHintText.setText(getString(
                    latestZeroEnabledState
                            ? R.string.zero_search_hint
                            : R.string.search_hint));
        }
    }

    private void showZeroInfoDialog(ZeroConfig zeroConfig) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this)
                .setMessage(buildZeroDialogMessage(zeroConfig.getMessage(), getString(R.string.zero_learn_more)))
                .setPositiveButton(getString(R.string.zero_learn_more_learn_more), getZeroMoreInfoListener())
                .setNegativeButton(getString(R.string.zero_learn_more_dismiss), getDismissClickListener());
        AlertDialog dialog = alert.create();
        dialog.show();
    }

    private CharSequence buildZeroDialogMessage(String title, String warning) {
        return Html.fromHtml("<b>" + title + "</b><br/><br/>" + warning);
    }

    private DialogInterface.OnClickListener getZeroMoreInfoListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                visitInExternalBrowser(MainActivity.this, (Uri.parse(getString(R.string.zero_webpage_url))));
                zeroFunnel.logExtLinkMore();
            }
        };
    }

    private DialogInterface.OnClickListener getDismissClickListener() {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        };
    }

    private boolean leftZeroRatedNetwork(boolean newZeroEnabledState) {
        return !newZeroEnabledState && isZeroEnabled;
    }

    private boolean enteredNewZeroRatedNetwork(ZeroConfig newZeroConfig, boolean newZeroEnabledState) {
        return newZeroEnabledState && (!isZeroEnabled || zeroConfigChanged(newZeroConfig));
    }

    private boolean zeroConfigChanged(ZeroConfig newConfig) {
        return !currentZeroConfig.equals(newConfig);
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
        outState.putBoolean("isSearching", isSearching());
        outState.putString(LANGUAGE_CODE_BUNDLE_KEY, app.getAppOrSystemLanguageCode());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (settingsActivityRequested(requestCode)) {
            handleSettingsActivityResult(resultCode);
        } else if (loginActivityRequested(requestCode)) {
            handleLoginActivityResult(resultCode);
        } else if (newArticleLanguageSelected(requestCode, resultCode) || galleryFilePageSelected(requestCode, resultCode)) {
            handleLangLinkOrFilePageResult(data);
        } else if (voiceSearchRequested(requestCode)) {
            handleVoiceSearchResult(resultCode, data);
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
        app.getSessionFunnel().persistSession();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        unregisterBus();
        super.onDestroy();
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
        if (currentActionMode.shouldInjectCustomMenu(MainActivity.this)) {
            currentActionMode.injectCustomMenu(MainActivity.this);
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

    private boolean voiceSearchRequested(int requestCode) {
        return requestCode == ACTIVITY_REQUEST_VOICE_SEARCH;
    }

    private void handleVoiceSearchResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null
                && data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            String searchQuery = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            openSearchFromIntent(searchQuery, SearchArticlesFragment.InvokeSource.VOICE);
        }
    }

    /**
     * Reload the main page in the new language, after delaying for one second in order to:
     * (1) Make sure that onStart in MainActivity gets called, thus registering the activity for the bus.
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST:
                if (PermissionUtil.isPermitted(grantResults)) {
                    if (pendingDownloadImage != null) {
                        new MediaDownloadReceiver(this).download(pendingDownloadImage);
                        setPendingDownload(null);
                    }
                } else {
                    setPendingDownload(null);
                    L.i("Write permission was denied by user");
                    FeedbackUtil.showMessage(this,
                            R.string.gallery_save_image_write_permission_rationale);
                }
                break;
            default:
                setPendingDownload(null);
                throw new RuntimeException("unexpected permission request code " + requestCode);
        }
    }

    private void setPendingDownload(@Nullable FeaturedImage image) {
        pendingDownloadImage = image;
    }
}
