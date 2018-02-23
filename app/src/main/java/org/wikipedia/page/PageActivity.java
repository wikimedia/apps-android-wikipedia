package org.wikipedia.page;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.hockeyapp.android.metrics.MetricsManager;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.IntentFunnel;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.descriptions.DescriptionEditRevertHelpView;
import org.wikipedia.events.ArticleSavedOrDeletedEvent;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.feed.mainpage.MainPageClient;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.language.LangLinksActivity;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.tabs.TabsProvider;
import org.wikipedia.page.tabs.TabsProvider.TabPosition;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.search.SearchFragment;
import org.wikipedia.search.SearchInvokeSource;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.theme.ThemeChooserDialog;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;
import org.wikipedia.wiktionary.WiktionaryDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.wikipedia.settings.Prefs.isLinkPreviewEnabled;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class PageActivity extends BaseActivity implements PageFragment.Callback,
        LinkPreviewDialog.Callback, SearchFragment.Callback, ThemeChooserDialog.Callback,
        WiktionaryDialog.Callback {

    public static final String ACTION_LOAD_IN_NEW_TAB = "org.wikipedia.load_in_new_tab";
    public static final String ACTION_LOAD_IN_CURRENT_TAB = "org.wikipedia.load_in_current_tab";
    public static final String ACTION_LOAD_FROM_EXISTING_TAB = "org.wikipedia.load_from_existing_tab";
    public static final String ACTION_SHOW_TAB_LIST = "org.wikipedia.show_tab_list";
    public static final String ACTION_RESUME_READING = "org.wikipedia.resume_reading";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";
    public static final String ACTION_APP_SHORTCUT = "org.wikipedia.app_shortcut";

    private static final String LANGUAGE_CODE_BUNDLE_KEY = "language";

    @BindView(R.id.tabs_container) View tabsContainerView;
    @BindView(R.id.page_progress_bar) ProgressBar progressBar;
    @BindView(R.id.page_toolbar_container) View toolbarContainerView;
    @BindView(R.id.page_toolbar) Toolbar toolbar;
    @BindView(R.id.page_toolbar_button_search) ImageView searchButton;
    @BindView(R.id.page_toolbar_button_show_tabs) ImageView tabsButton;
    @Nullable private Unbinder unbinder;

    private PageFragment pageFragment;

    private WikipediaApp app;
    @Nullable private Bus bus;
    private EventBusMethods busMethods;
    private ActionMode currentActionMode;

    private PageToolbarHideHandler toolbarHideHandler;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    private DialogInterface.OnDismissListener listDialogDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialogInterface) {
            pageFragment.updateBookmarkAndMenuOptionsFromDao();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getApplicationContext();
        MetricsManager.register(app);
        app.checkCrashes(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        try {
            setContentView(R.layout.activity_page);
        } catch (Exception e) {
            if (e.getMessage().contains("WebView")) {
                // If the system failed to inflate our activity because of the WebView (which could
                // be one of several types of exceptions), it likely means that the system WebView
                // is in the process of being updated. In this case, show the user a message and
                // bail immediately.
                Toast.makeText(app, R.string.error_webview_updating, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            throw e;
        }

        unbinder = ButterKnife.bind(this);

        busMethods = new EventBusMethods();
        registerBus();

        updateProgressBar(false, true, 0);

        pageFragment = (PageFragment) getSupportFragmentManager().findFragmentById(R.id.page_fragment);

        setStatusBarColor(ResourceUtil.getThemedAttributeId(this, R.attr.page_status_bar_color));
        setSupportActionBar(toolbar);
        clearActionBarTitle();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FeedbackUtil.setToolbarButtonLongPressToast(searchButton, tabsButton);
        tabsButton.setImageDrawable(ContextCompat.getDrawable(pageFragment.getContext(),
                ResourceUtil.getTabListIcon(pageFragment.getTabCount())));

        toolbarHideHandler = new PageToolbarHideHandler(pageFragment, toolbarContainerView, toolbar, tabsButton);

        boolean languageChanged = false;
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("isSearching")) {
                openSearchFragment(SearchInvokeSource.TOOLBAR, null);
            }
            String language = savedInstanceState.getString(LANGUAGE_CODE_BUNDLE_KEY);
            languageChanged = !app.getAppOrSystemLanguageCode().equals(language);

            // Note: when system language is enabled, and the system language is changed outside of
            // the app, MRU languages are not updated. There's no harm in doing that here but since
            // the user didn't choose that language in app, it may be unexpected.
        }

        if (languageChanged) {
            app.resetWikiSite();
            loadMainPageInForegroundTab();
        }

        if (savedInstanceState == null) {
            // if there's no savedInstanceState, and we're not coming back from a Theme change,
            // then we must have been launched with an Intent, so... handle it!
            handleIntent(getIntent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isSearching()) {
            getMenuInflater().inflate(R.menu.menu_page_actions, menu);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (isSearching()) {
            return false;
        }

        MenuItem otherLangItem = menu.findItem(R.id.menu_page_other_languages);
        MenuItem shareItem = menu.findItem(R.id.menu_page_share);
        MenuItem addToListItem = menu.findItem(R.id.menu_page_add_to_list);
        MenuItem removeFromListsItem = menu.findItem(R.id.menu_page_remove_from_list);
        MenuItem findInPageItem = menu.findItem(R.id.menu_page_find_in_page);
        MenuItem contentIssues = menu.findItem(R.id.menu_page_content_issues);
        MenuItem similarTitles = menu.findItem(R.id.menu_page_similar_titles);
        MenuItem themeChooserItem = menu.findItem(R.id.menu_page_font_and_theme);

        if (pageFragment.isLoading() || pageFragment.getErrorState()) {
            otherLangItem.setEnabled(false);
            shareItem.setEnabled(false);
            addToListItem.setEnabled(false);
            findInPageItem.setEnabled(false);
            contentIssues.setEnabled(false);
            similarTitles.setEnabled(false);
            themeChooserItem.setEnabled(false);
            removeFromListsItem.setEnabled(false);
        } else {
            // Only display "Read in other languages" if the article is in other languages
            otherLangItem.setVisible(pageFragment.getPage() != null && pageFragment.getPage().getPageProperties().getLanguageCount() != 0);
            otherLangItem.setEnabled(true);
            shareItem.setEnabled(pageFragment.getPage() != null && pageFragment.getPage().isArticle());
            addToListItem.setEnabled(pageFragment.getPage() != null && pageFragment.getPage().isArticle());
            removeFromListsItem.setVisible(pageFragment.isPresentInOfflineLists());
            removeFromListsItem.setEnabled(pageFragment.isPresentInOfflineLists());
            findInPageItem.setEnabled(true);
            themeChooserItem.setEnabled(true);
            updateMenuPageInfo(menu);
        }
        return true;
    }

    @OnClick(R.id.page_toolbar_button_search)
    public void onSearchButtonClicked() {
        openSearchFragment(SearchInvokeSource.TOOLBAR, null);
    }

    @OnClick(R.id.page_toolbar_button_show_tabs)
    public void onShowTabsButtonClicked() {
        pageFragment.enterTabMode(false);
    }

    private void finishActionMode() {
        currentActionMode.finish();
    }

    public void hideSoftKeyboard() {
        DeviceUtil.hideSoftKeyboard(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (shouldRecreateMainActivity()) {
                    startActivity(getSupportParentActivityIntent()
                            .putExtra(Constants.INTENT_RETURN_TO_MAIN, true));
                }
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSearchRequested() {
        showToolbar();
        openSearchFragment(SearchInvokeSource.TOOLBAR, null);
        return true;
    }

    public void showToolbar() {
        // TODO: make toolbar visible, via CoordinatorLayout
    }

    /** @return True if the contextual action bar is open. */
    public boolean isCabOpen() {
        return currentActionMode != null;
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(ACTION_RESUME_READING).setClass(context, PageActivity.class);
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull String title) {
        PageTitle pageTitle = new PageTitle(title, WikipediaApp.getInstance().getWikiSite());
        return newIntentForNewTab(context, new HistoryEntry(pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK), pageTitle);
    }

    @NonNull
    public static Intent newIntentForNewTab(@NonNull Context context,
                                            @NonNull HistoryEntry entry,
                                            @NonNull PageTitle title) {
        return new Intent(ACTION_LOAD_IN_NEW_TAB)
                .setClass(context, PageActivity.class)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, title);
    }

    public static Intent newIntentForCurrentTab(@NonNull Context context,
                                                @NonNull HistoryEntry entry,
                                                @NonNull PageTitle title) {
        return new Intent(ACTION_LOAD_IN_CURRENT_TAB)
                .setClass(context, PageActivity.class)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, title);
    }

    public static Intent newIntentForExistingTab(@NonNull Context context,
                                                 @NonNull HistoryEntry entry,
                                                 @NonNull PageTitle title) {
        return new Intent(ACTION_LOAD_FROM_EXISTING_TAB)
                .setClass(context, PageActivity.class)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, title);
    }

    @NonNull
    public static Intent newIntentForTabList(@NonNull Context context) {
        return new Intent(ACTION_SHOW_TAB_LIST).setClass(context, PageActivity.class);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@NonNull Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            WikiSite wiki = new WikiSite(intent.getData());
            PageTitle title = wiki.titleForUri(intent.getData());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_EXTERNAL_LINK);
            loadPageInForegroundTab(title, historyEntry);
        } else if (ACTION_LOAD_IN_NEW_TAB.equals(intent.getAction())
                || ACTION_LOAD_IN_CURRENT_TAB.equals(intent.getAction())) {
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
            if (ACTION_LOAD_IN_NEW_TAB.equals(intent.getAction())) {
                loadPageInForegroundTab(title, historyEntry);
            } else if (ACTION_LOAD_IN_CURRENT_TAB.equals(intent.getAction())) {
                loadPage(title, historyEntry, TabPosition.CURRENT_TAB);
            }
            if (intent.hasExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER)) {
                showDescriptionEditRevertDialog(intent.getStringExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER));
            }
        } else if (ACTION_LOAD_FROM_EXISTING_TAB.equals(intent.getAction())) {
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
            loadPage(title, historyEntry, TabPosition.EXISTING_TAB);
        } else if (ACTION_SHOW_TAB_LIST.equals(intent.getAction())
                || ACTION_RESUME_READING.equals(intent.getAction())
                || intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            // do nothing, since this will be handled indirectly by PageFragment.
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            PageTitle title = new PageTitle(query, app.getWikiSite());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
            loadPageInForegroundTab(title, historyEntry);
        } else if (intent.hasExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET)) {
            new IntentFunnel(app).logFeaturedArticleWidgetTap();
            loadMainPageInForegroundTab();
        } else {
            loadMainPageInCurrentTab();
        }
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
        SearchFragment searchFragment = searchFragment();
        return searchFragment != null && searchFragment.isSearchActive();
    }

    /**
     * Load a new page, and put it on top of the backstack.
     * @param title Title of the page to load.
     * @param entry HistoryEntry associated with this page.
     */
    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        loadPage(title, entry, TabPosition.CURRENT_TAB);
    }

    /**
     * Load a new page, and put it on top of the backstack, optionally allowing state loss of the
     * fragment manager. Useful for when this function is called from an AsyncTask result.
     * @param title Title of the page to load.
     * @param entry HistoryEntry associated with this page.
     * @param position Whether to open this page in the current tab, a new background tab, or new
     *                 foreground tab.
     */
    public void loadPage(@NonNull final PageTitle title,
                         @NonNull final HistoryEntry entry,
                         @NonNull final TabPosition position) {
        if (isDestroyed()) {
            return;
        }

        if (entry.getSource() != HistoryEntry.SOURCE_INTERNAL_LINK || !isLinkPreviewEnabled()) {
            new LinkPreviewFunnel(app, entry.getSource()).logNavigate();
        }

        app.putCrashReportProperty("api", title.getWikiSite().authority());
        app.putCrashReportProperty("title", title.toString());

        if (title.isSpecial()) {
            visitInExternalBrowser(this, Uri.parse(title.getMobileUri()));
            return;
        }

        tabsContainerView.post(() -> {
            if (!pageFragment.isAdded()) {
                return;
            }
            // Close the link preview, if one is open.
            hideLinkPreview();

            pageFragment.closeFindInPage();
            if (position == TabPosition.CURRENT_TAB) {
                pageFragment.loadPage(title, entry, true);
            } else if (position == TabPosition.NEW_TAB_BACKGROUND) {
                pageFragment.openInNewBackgroundTabFromMenu(title, entry);
            } else if (position == TabPosition.EXISTING_TAB) {
                pageFragment.openFromExistingTab(title, entry);
            } else {
                pageFragment.openInNewForegroundTabFromMenu(title, entry);
            }
            app.getSessionFunnel().pageViewed(entry);
        });
    }

    public void loadPageInForegroundTab(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        loadPage(title, entry, TabPosition.NEW_TAB_FOREGROUND);
    }

    public void loadMainPageInForegroundTab() {
        loadMainPage(TabPosition.NEW_TAB_FOREGROUND);
    }

    private void loadMainPageInCurrentTab() {
        loadMainPage(TabPosition.CURRENT_TAB);
    }

    /**
     * Go directly to the Main Page of the current Wiki, optionally allowing state loss of the
     * fragment manager. Useful for when this function is called from an AsyncTask result.
     */
    public void loadMainPage(TabPosition position) {
        PageTitle title = MainPageClient.getMainPageTitle();
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE);
        loadPage(title, historyEntry, position);
    }

    public void showLinkPreview(@NonNull PageTitle title, int entrySource) {
        showLinkPreview(title, entrySource, null);
    }

    public void showLinkPreview(@NonNull PageTitle title, int entrySource, @Nullable Location location) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                LinkPreviewDialog.newInstance(title, entrySource, location));
    }

    private void hideLinkPreview() {
        bottomSheetPresenter.dismiss(getSupportFragmentManager());
    }

    public void showAddToListDialog(@NonNull PageTitle title, @NonNull AddToReadingListDialog.InvokeSource source) {
        bottomSheetPresenter.showAddToListDialog(getSupportFragmentManager(), title, source, listDialogDismissListener);
    }

    // Note: back button first handled in {@link #onOptionsItemSelected()};
    @Override
    public void onBackPressed() {
        if (isCabOpen()) {
            finishActionMode();
            return;
        }

        SearchFragment searchFragment = searchFragment();
        if (searchFragment != null && searchFragment.onBackPressed()) {
            if (searchFragment.isLaunchedFromIntent()) {
                finish();
            }
            return;
        }
        app.getSessionFunnel().backPressed();
        if (pageFragment.onBackPressed()) {
            return;
        }
        finish();
    }

    @Override
    public void onPageShowBottomSheet(@NonNull BottomSheetDialog dialog) {
        bottomSheetPresenter.show(getSupportFragmentManager(), dialog);
    }

    @Override
    public void onPageShowBottomSheet(@NonNull BottomSheetDialogFragment dialog) {
        bottomSheetPresenter.show(getSupportFragmentManager(), dialog);
    }

    @Override
    public void onPageDismissBottomSheet() {
        bottomSheetPresenter.dismiss(getSupportFragmentManager());
    }

    @Override
    public void onPageInitWebView(@NonNull ObservableWebView webView) {
        toolbarHideHandler.setScrollView(webView);
    }

    @Override
    public void onPageLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        loadPage(title, entry);
    }

    @Override
    public void onPageShowLinkPreview(@NonNull PageTitle title, int source) {
        showLinkPreview(title, source);
    }

    @Override
    public void onPageLoadMainPageInForegroundTab() {
        loadMainPageInForegroundTab();
    }

    @Override
    public void onPageUpdateProgressBar(boolean visible, boolean indeterminate, int value) {
        updateProgressBar(visible, indeterminate, value);
    }

    @Override
    public void onPageShowThemeChooser() {
        bottomSheetPresenter.show(getSupportFragmentManager(), new ThemeChooserDialog());
    }

    @Nullable
    @Override
    public void onPageStartSupportActionMode(@NonNull ActionMode.Callback callback) {
        startActionMode(callback);
    }

    @Override
    public void onPageShowToolbar() {
        showToolbar();
    }

    @Override
    public void onPageHideSoftKeyboard() {
        hideSoftKeyboard();
    }

    @Override
    public void onPageAddToReadingList(@NonNull PageTitle title,
                                @NonNull AddToReadingListDialog.InvokeSource source) {
        showAddToListDialog(title, source);
    }

    @Override
    public void onPageRemoveFromReadingLists(@NonNull PageTitle title) {
        if (!pageFragment.isAdded()) {
            return;
        }
        FeedbackUtil.showMessage(this,
                getString(R.string.reading_list_item_deleted, title.getDisplayText()));
        pageFragment.updateBookmarkAndMenuOptionsFromDao();
    }

    @Override
    public void onPagePopFragment() {
        finish();
    }

    @Override
    public void onPageInvalidateOptionsMenu() {
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onPageLoadError(@NonNull PageTitle title) {
        getSupportActionBar().setTitle(title.getDisplayText());
    }

    @Override
    public void onPageLoadErrorBackPressed() {
        finish();
    }

    @Override
    public void onSearchSelectPage(@NonNull HistoryEntry entry, boolean inNewTab) {
        loadPage(entry.getTitle(), entry, inNewTab ? TabsProvider.TabPosition.NEW_TAB_BACKGROUND
                : TabsProvider.TabPosition.CURRENT_TAB);
    }

    @Override
    public void onPageHideAllContent() {
        toolbarHideHandler.setFadeEnabled(false);
    }

    @Override
    public void onPageSetToolbarFadeEnabled(boolean enabled) {
        toolbarHideHandler.setFadeEnabled(enabled);
    }

    @Override
    public void onPageSetToolbarForceNoFace(boolean force) {
        toolbarHideHandler.setForceNoFade(force);
    }

    @Override
    public void onSearchOpen() {
        toolbarContainerView.setVisibility(View.GONE);
    }

    @Override
    public void onSearchClose(boolean launchedFromIntent) {
        SearchFragment fragment = searchFragment();
        if (fragment != null) {
            closeSearchFragment(fragment);
        }
        toolbarContainerView.setVisibility(View.VISIBLE);
        hideSoftKeyboard();
    }

    @Override
    public void onSearchResultAddToList(@NonNull PageTitle title, @NonNull AddToReadingListDialog.InvokeSource source) {
        showAddToListDialog(title, source);
    }

    @Override
    public void onSearchResultShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    @Override
    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        loadPage(title, entry, inNewTab ? TabPosition.NEW_TAB_BACKGROUND : TabPosition.CURRENT_TAB);
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getCanonicalUri());
        showCopySuccessMessage();
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        showAddToListDialog(title, AddToReadingListDialog.InvokeSource.LINK_PREVIEW_MENU);
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    @Override
    public void onSearchResultCopyLink(@NonNull PageTitle title) {
        copyLink(title.getCanonicalUri());
        showCopySuccessMessage();
    }

    @Override
    public void wiktionaryShowDialogForTerm(@NonNull String term) {
        pageFragment.getShareHandler().showWiktionaryDefinition(term);
    }

    @Override
    public void onToggleDimImages() {
        recreate();
    }

    @Override
    public void onCancel() { }

    private void copyLink(@NonNull String url) {
        ClipboardUtil.setPlainText(this, null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    private boolean shouldRecreateMainActivity() {
        return getIntent().getAction() == null
                || getIntent().getAction().equals(Intent.ACTION_VIEW);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.resetWikiSite();
        app.getSessionFunnel().touchSession();
    }

    @Override
    public void onPause() {
        if (isCabOpen()) {
            // Explicitly close any current ActionMode (see T147191)
            finishActionMode();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState(outState);
    }

    private void saveState(@NonNull Bundle outState) {
        outState.putBoolean("isSearching", isSearching());
        outState.putString(LANGUAGE_CODE_BUNDLE_KEY, app.getAppOrSystemLanguageCode());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (settingsActivityRequested(requestCode)) {
            handleSettingsActivityResult(resultCode);
        } else if (newArticleLanguageSelected(requestCode, resultCode) || galleryPageSelected(requestCode, resultCode)) {
            handleLangLinkOrPageResult(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleLangLinkOrPageResult(final Intent data) {
        tabsContainerView.post(() -> handleIntent(data));
    }

    @Override
    protected void onStop() {
        app.getSessionFunnel().persistSession();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (unbinder != null) {
            unbinder.unbind();
        }
        unregisterBus();
        super.onDestroy();
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        if (!isSearching() && !isCabOpen() && mode.getTag() == null) {
            Menu menu = mode.getMenu();
            menu.clear();
            mode.getMenuInflater().inflate(R.menu.menu_text_select, menu);
            pageFragment.onActionModeShown(mode);
        }
    }

    @Override
    public void onActionModeFinished(android.view.ActionMode mode) {
        super.onActionModeFinished(mode);
        currentActionMode = null;
        toolbarHideHandler.onScrolled(pageFragment.getWebView().getScrollY(),
                pageFragment.getWebView().getScrollY());
    }

    protected void clearActionBarTitle() {
        getSupportActionBar().setTitle("");
    }

    private void registerBus() {
        bus = app.getBus();
        bus.register(busMethods);
        L.d("Registered bus.");
    }

    private void unregisterBus() {
        if (bus != null) {
            bus.unregister(busMethods);
        }
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

    private boolean newArticleLanguageSelected(int requestCode, int resultCode) {
        return requestCode == Constants.ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT;
    }

    private boolean galleryPageSelected(int requestCode, int resultCode) {
        return requestCode == Constants.ACTIVITY_REQUEST_GALLERY && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED;
    }

    private boolean languageChanged(int resultCode) {
        return resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED;
    }

    /**
     * Reload the main page in the new language, after delaying for one second in order to:
     * (1) Make sure that onStart in MainActivity gets called, thus registering the activity for the bus.
     * (2) Ensure a smooth transition, which is very jarring without a delay.
     */
    private void loadNewLanguageMainPage() {
        Handler uiThread = new Handler(Looper.getMainLooper());
        uiThread.postDelayed(() -> {
            loadMainPageInForegroundTab();
            updateFeaturedPageWidget();
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

    private void showDescriptionEditRevertDialog(@NonNull String qNumber) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_reverted_title)
                .setView(new DescriptionEditRevertHelpView(this, qNumber))
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }

    @SuppressLint("CommitTransaction")
    private void openSearchFragment(@NonNull SearchInvokeSource source, @Nullable String query) {
        Fragment fragment = searchFragment();
        if (fragment == null) {
            fragment = SearchFragment.newInstance(source, StringUtils.trim(query));
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.activity_page_container, fragment)
                    .commitNowAllowingStateLoss();
        }
    }

    private void updateMenuPageInfo(@NonNull Menu menu) {
        MenuItem contentIssues = menu.findItem(R.id.menu_page_content_issues);
        MenuItem similarTitles = menu.findItem(R.id.menu_page_similar_titles);
        PageInfo pageInfo = pageFragment.getPageInfo();
        contentIssues.setVisible(pageInfo != null && pageInfo.hasContentIssues());
        contentIssues.setEnabled(true);
        similarTitles.setVisible(pageInfo != null && pageInfo.hasSimilarTitles());
        similarTitles.setEnabled(true);
    }

    @SuppressLint("CommitTransaction")
    private void closeSearchFragment(@NonNull SearchFragment fragment) {
        getSupportFragmentManager().beginTransaction().remove(fragment).commitNowAllowingStateLoss();
    }

    @Nullable private SearchFragment searchFragment() {
        return (SearchFragment) getSupportFragmentManager()
                .findFragmentById(R.id.activity_page_container);
    }

    @NonNull public TabLayout getTabLayout() {
        return pageFragment.getTabLayout();
    }

    private class EventBusMethods {
        @Subscribe public void on(ChangeTextSizeEvent event) {
            if (pageFragment != null && pageFragment.getWebView() != null) {
                pageFragment.updateFontSize();
            }
        }

        @Subscribe public void on(@NonNull ArticleSavedOrDeletedEvent event) {
            if (pageFragment == null || !pageFragment.isAdded()) {
                return;
            }
            for (ReadingListPage page : event.getPages()) {
                if (page.title().equals(pageFragment.getTitleOriginal().getDisplayText())) {
                    pageFragment.updateBookmarkAndMenuOptionsFromDao();
                }
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_F)
                || (!event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_F3)) {
            pageFragment.showFindInPage();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
