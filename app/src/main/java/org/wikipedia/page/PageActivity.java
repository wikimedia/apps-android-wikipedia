package org.wikipedia.page;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.FixedDrawerLayout;
import androidx.preference.PreferenceManager;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.IntentFunnel;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.analytics.WatchlistFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.commons.FilePageActivity;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.descriptions.DescriptionEditRevertHelpView;
import org.wikipedia.events.ArticleSavedOrDeletedEvent;
import org.wikipedia.events.ChangeTextSizeEvent;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.language.LangLinksActivity;
import org.wikipedia.main.MainActivity;
import org.wikipedia.navtab.NavTab;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.tabs.TabActivity;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.search.SearchActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.suggestededits.SuggestedEditsSnackbars;
import org.wikipedia.talk.TalkTopicsActivity;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ReleaseUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.views.FrameLayoutNavMenuTriggerer;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.PageActionOverflowView;
import org.wikipedia.views.TabCountsView;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiArticleCardView;
import org.wikipedia.watchlist.WatchlistExpiry;
import org.wikipedia.widgets.WidgetProviderFeaturedPage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Consumer;

import static org.wikipedia.Constants.ACTIVITY_REQUEST_SETTINGS;
import static org.wikipedia.Constants.INTENT_EXTRA_ACTION;
import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.TOOLBAR;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditSuccessActivity.RESULT_OK_FROM_EDIT_SUCCESS;
import static org.wikipedia.descriptions.DescriptionEditTutorialActivity.DESCRIPTION_SELECTED_TEXT;
import static org.wikipedia.settings.Prefs.isLinkPreviewEnabled;
import static org.wikipedia.settings.SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class PageActivity extends BaseActivity implements PageFragment.Callback,
        LinkPreviewDialog.Callback, FrameLayoutNavMenuTriggerer.Callback {
    public static final String ACTION_LOAD_IN_NEW_TAB = "org.wikipedia.load_in_new_tab";
    public static final String ACTION_LOAD_IN_CURRENT_TAB = "org.wikipedia.load_in_current_tab";
    public static final String ACTION_LOAD_IN_CURRENT_TAB_SQUASH = "org.wikipedia.load_in_current_tab_squash";
    public static final String ACTION_LOAD_FROM_EXISTING_TAB = "org.wikipedia.load_from_existing_tab";
    public static final String ACTION_CREATE_NEW_TAB = "org.wikipedia.create_new_tab";
    public static final String ACTION_RESUME_READING = "org.wikipedia.resume_reading";
    public static final String EXTRA_PAGETITLE = "org.wikipedia.pagetitle";
    public static final String EXTRA_HISTORYENTRY  = "org.wikipedia.history.historyentry";

    private static final String LANGUAGE_CODE_BUNDLE_KEY = "language";

    public enum TabPosition {
        CURRENT_TAB,
        CURRENT_TAB_SQUASH,
        NEW_TAB_BACKGROUND,
        NEW_TAB_FOREGROUND,
        EXISTING_TAB
    }

    @BindView(R.id.navigation_drawer) FixedDrawerLayout drawerLayout;
    @BindView(R.id.activity_page_container) FrameLayoutNavMenuTriggerer containerWithNavTrigger;
    @BindView(R.id.page_progress_bar) ProgressBar progressBar;
    @BindView(R.id.page_toolbar_container) View toolbarContainerView;
    @BindView(R.id.page_toolbar) Toolbar toolbar;
    @BindView(R.id.page_toolbar_button_tabs) TabCountsView tabsButton;
    @BindView(R.id.page_toolbar_button_show_overflow_menu) ImageView overflowButton;
    @BindView(R.id.page_fragment) View pageFragmentView;
    @BindView(R.id.page_toolbar_button_search) View searchButton;
    @BindView(R.id.wiki_article_card_view) WikiArticleCardView wikiArticleCardView;
    @Nullable private Unbinder unbinder;

    private PageFragment pageFragment;
    private boolean hasTransitionAnimation;
    private boolean wasTransitionShown;

    private WikipediaApp app;
    private Set<ActionMode> currentActionModes = new HashSet<>();
    private CompositeDisposable disposables = new CompositeDisposable();

    private ViewHideHandler toolbarHideHandler;
    private OverflowCallback overflowCallback = new OverflowCallback();
    private final WatchlistFunnel watchlistFunnel = new WatchlistFunnel();

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

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        try {
            setContentView(R.layout.activity_page);
        } catch (Exception e) {
            if ((!TextUtils.isEmpty(e.getMessage()) && e.getMessage().toLowerCase().contains("webview"))
                    || (!TextUtils.isEmpty(ThrowableUtil.getInnermostThrowable(e).getMessage())
                    && ThrowableUtil.getInnermostThrowable(e).getMessage().toLowerCase().contains("webview"))) {
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

        disposables.add(app.getBus().subscribe(new EventBusConsumer()));

        updateProgressBar(false);

        pageFragment = (PageFragment) getSupportFragmentManager().findFragmentById(R.id.page_fragment);

        setSupportActionBar(toolbar);
        clearActionBarTitle();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        searchButton.setOnClickListener(view -> startActivity(SearchActivity.newIntent(PageActivity.this, TOOLBAR, null)));

        tabsButton.setColor(ResourceUtil.getThemedColor(this, R.attr.material_theme_de_emphasised_color));
        FeedbackUtil.setButtonLongPressToast(tabsButton, overflowButton);
        tabsButton.updateTabCount(false);

        toolbarHideHandler = new ViewHideHandler(toolbarContainerView, null, Gravity.TOP);

        drawerLayout.setScrimColor(Color.TRANSPARENT);
        containerWithNavTrigger.setCallback(this);

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            toolbarContainerView.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
            pageFragment.updateInsets(insets);
            return insets;
        });

        hasTransitionAnimation = getIntent().getBooleanExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, false);
        wikiArticleCardView.setVisibility(hasTransitionAnimation ? View.VISIBLE : View.GONE);

        boolean languageChanged = false;
        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("isSearching")) {
                startActivity(SearchActivity.newIntent(this, TOOLBAR, null));
            }
            String language = savedInstanceState.getString(LANGUAGE_CODE_BUNDLE_KEY);
            languageChanged = !app.getAppOrSystemLanguageCode().equals(language);
        }

        if (languageChanged) {
            app.resetWikiSite();
            loadMainPage(TabPosition.EXISTING_TAB);
        }

        if (savedInstanceState == null) {
            // if there's no savedInstanceState, and we're not coming back from a Theme change,
            // then we must have been launched with an Intent, so... handle it!
            handleIntent(getIntent());
        }
    }

    @OnClick(R.id.page_toolbar_button_tabs)
    public void onShowTabsButtonClicked() {
        TabActivity.captureFirstTabBitmap(pageFragment.getContainerView());
        startActivityForResult(TabActivity.newIntentFromPageActivity(this), Constants.ACTIVITY_REQUEST_BROWSE_TABS);
    }

    @OnClick(R.id.page_toolbar_button_show_overflow_menu)
    public void onShowOverflowMenuButtonClicked() {
        showOverflowMenu(toolbar.findViewById(R.id.page_toolbar_button_show_overflow_menu));
    }

    public void animateTabsButton() {
        tabsButton.updateTabCount(true);
    }

    public void hideSoftKeyboard() {
        DeviceUtil.hideSoftKeyboard(this);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isDestroyed()) {
            tabsButton.updateTabCount(false);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (app.haveMainActivity()) {
                    onBackPressed();
                } else {
                    goToMainTab(NavTab.EXPLORE);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onNavMenuSwipeRequest(int gravity) {
        if (!isCabOpen() && gravity == Gravity.END) {
            pageFragment.getTocHandler().show();
        }
    }

    private void goToMainTab(@NonNull NavTab tab) {
        startActivity(MainActivity.newIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
                .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, tab.code()));
        finish();
    }

    /** @return True if the contextual action bar is open. */
    private boolean isCabOpen() {
        return !currentActionModes.isEmpty();
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(ACTION_RESUME_READING).setClass(context, PageActivity.class);
    }

    @NonNull
    public static Intent newIntentForNewTab(@NonNull Context context) {
        return new Intent(ACTION_CREATE_NEW_TAB)
                .setClass(context, PageActivity.class);
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
        return newIntentForCurrentTab(context, entry, title, true);
    }

    public static Intent newIntentForCurrentTab(@NonNull Context context,
                                                @NonNull HistoryEntry entry,
                                                @NonNull PageTitle title,
                                                boolean squashBackstack) {
        return new Intent(squashBackstack ? ACTION_LOAD_IN_CURRENT_TAB_SQUASH : ACTION_LOAD_IN_CURRENT_TAB)
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(@NonNull Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri uri = intent.getData();
            if (ReleaseUtil.isProdRelease() && uri.getScheme() != null && uri.getScheme().equals("http")) {
                // For external links, ensure that they're using https.
                uri = uri.buildUpon().scheme(WikiSite.DEFAULT_SCHEME).build();
            }
            WikiSite wiki = new WikiSite(uri);
            PageTitle title = wiki.titleForUri(uri);
            HistoryEntry historyEntry = new HistoryEntry(title,
                    intent.hasExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID)
                            ? HistoryEntry.SOURCE_NOTIFICATION_SYSTEM : HistoryEntry.SOURCE_EXTERNAL_LINK);
            if (intent.hasExtra(Intent.EXTRA_REFERRER)) {
                // Populate the referrer with the externally-referring URL, e.g. an external Browser URL.
                // This can be a Uri or a String, so let's extract it safely as an Object.
                historyEntry.setReferrer(intent.getExtras().get(Intent.EXTRA_REFERRER).toString());
            }
            // Special cases:
            // If the link is to a page in the "donate." or "thankyou." domains (e.g. a "thank you" page
            // after having donated), then bounce it out to an external browser, since we don't have
            // the same cookie state as the browser does.
            String language = wiki.languageCode().toLowerCase();
            boolean isDonationRelated = language.equals("donate") || language.equals("thankyou");
            if (isDonationRelated) {
                visitInExternalBrowser(this, uri);
                finish();
                return;
            }
            loadPage(title, historyEntry, TabPosition.NEW_TAB_FOREGROUND);
        } else if ((ACTION_LOAD_IN_NEW_TAB.equals(intent.getAction())
                || ACTION_LOAD_IN_CURRENT_TAB.equals(intent.getAction())
                || ACTION_LOAD_IN_CURRENT_TAB_SQUASH.equals(intent.getAction()))
                && intent.hasExtra(EXTRA_HISTORYENTRY)) {
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
            if (ACTION_LOAD_IN_NEW_TAB.equals(intent.getAction())) {
                loadPage(title, historyEntry, TabPosition.NEW_TAB_FOREGROUND);
            } else if (ACTION_LOAD_IN_CURRENT_TAB.equals(intent.getAction())) {
                loadPage(title, historyEntry, TabPosition.CURRENT_TAB);
            } else if (ACTION_LOAD_IN_CURRENT_TAB_SQUASH.equals(intent.getAction())) {
                loadPage(title, historyEntry, TabPosition.CURRENT_TAB_SQUASH);
            }
            if (intent.hasExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER)) {
                showDescriptionEditRevertDialog(intent.getStringExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER));
            }
        } else if (ACTION_LOAD_FROM_EXISTING_TAB.equals(intent.getAction())
                && intent.hasExtra(EXTRA_HISTORYENTRY)) {
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = intent.getParcelableExtra(EXTRA_HISTORYENTRY);
            loadPage(title, historyEntry, TabPosition.EXISTING_TAB);
        } else if (ACTION_RESUME_READING.equals(intent.getAction())
                || intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            loadFilePageFromBackStackIfNeeded();
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            PageTitle title = new PageTitle(query, app.getWikiSite());
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_SEARCH);
            loadPage(title, historyEntry, TabPosition.EXISTING_TAB);
        } else if (intent.hasExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET)) {
            new IntentFunnel(app).logFeaturedArticleWidgetTap();
            PageTitle title = intent.getParcelableExtra(EXTRA_PAGETITLE);
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_WIDGET);
            loadPage(title, historyEntry, TabPosition.EXISTING_TAB);
        } else if (ACTION_CREATE_NEW_TAB.equals(intent.getAction())) {
            loadMainPage(TabPosition.NEW_TAB_FOREGROUND);
        } else {
            loadMainPage(TabPosition.CURRENT_TAB);
        }
    }

    public void updateProgressBar(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
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

        if (hasTransitionAnimation && !wasTransitionShown) {
            pageFragmentView.setVisibility(View.GONE);
            wikiArticleCardView.prepareForTransition(title);
            wasTransitionShown = true;
        }

        if (entry.getSource() != HistoryEntry.SOURCE_INTERNAL_LINK || !isLinkPreviewEnabled()) {
            new LinkPreviewFunnel(app, entry.getSource()).logNavigate();
        }

        app.putCrashReportProperty("api", title.getWikiSite().authority());
        app.putCrashReportProperty("title", title.toString());

        if (loadNonArticlePageIfNeeded(title)) {
            return;
        }

        toolbarContainerView.post(() -> {
            if (!pageFragment.isAdded()) {
                return;
            }

            // Close the link preview, if one is open.
            hideLinkPreview();

            onPageCloseActionMode();
            if (position == TabPosition.CURRENT_TAB) {
                pageFragment.loadPage(title, entry, true, false);
            } else if (position == TabPosition.CURRENT_TAB_SQUASH) {
                pageFragment.loadPage(title, entry, true, true);
            } else if (position == TabPosition.NEW_TAB_BACKGROUND) {
                pageFragment.openInNewBackgroundTab(title, entry);
            } else if (position == TabPosition.NEW_TAB_FOREGROUND) {
                pageFragment.openInNewForegroundTab(title, entry);
            } else {
                pageFragment.openFromExistingTab(title, entry);
            }
            app.getSessionFunnel().pageViewed(entry);
        });
    }

    public void loadMainPage(TabPosition position) {
        PageTitle title = new PageTitle(SiteInfoClient.getMainPageForLang(app.getAppOrSystemLanguageCode()), app.getWikiSite());
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE);
        loadPage(title, historyEntry, position);
    }

    private void loadFilePageFromBackStackIfNeeded() {
        if (!pageFragment.getCurrentTab().getBackStack().isEmpty()) {
            PageBackStackItem item = pageFragment.getCurrentTab().getBackStack().get(pageFragment.getCurrentTab().getBackStackPosition());
            loadNonArticlePageIfNeeded(item.getTitle());
        }
    }

    private boolean loadNonArticlePageIfNeeded(@Nullable PageTitle title) {
        if (title != null) {
            if (title.isFilePage()) {
                startActivity(FilePageActivity.newIntent(this, title));
                finish();
                return true;
            } else if (title.namespace() == Namespace.USER_TALK || title.namespace() == Namespace.TALK) {
                startActivity(TalkTopicsActivity.newIntent(this, title.pageTitleForTalkPage(), InvokeSource.PAGE_ACTIVITY));
                finish();
                return true;
            }
        }
        return false;
    }

    private void hideLinkPreview() {
        bottomSheetPresenter.dismiss(getSupportFragmentManager());
    }

    public void showAddToListDialog(@NonNull PageTitle title, @NonNull InvokeSource source) {
        bottomSheetPresenter.showAddToListDialog(getSupportFragmentManager(), title, source, listDialogDismissListener);
    }

    public void showMoveToListDialog(long sourceReadingListId, @NonNull PageTitle title, @NonNull InvokeSource source, boolean showDefaultList) {
        bottomSheetPresenter.showMoveToListDialog(getSupportFragmentManager(), sourceReadingListId, title, source, showDefaultList, listDialogDismissListener);
    }

    @Override
    public void onPageLoadComplete() {
        removeTransitionAnimState();
        maybeShowWatchlistTooltip();
    }

    private void removeTransitionAnimState() {
        if (pageFragmentView.getVisibility() != View.VISIBLE) {
            pageFragmentView.setVisibility(View.VISIBLE);
        }
        if (wikiArticleCardView.getVisibility() != View.GONE) {
            final int delayMillis = 250;
            wikiArticleCardView.postDelayed(() -> {
                if (wikiArticleCardView != null) {
                    wikiArticleCardView.setVisibility(View.GONE);
                }
            }, delayMillis);
        }
    }

    // Note: back button first handled in {@link #onOptionsItemSelected()};
    @Override
    public void onBackPressed() {
        if (isCabOpen()) {
            onPageCloseActionMode();
            return;
        }

        app.getSessionFunnel().backPressed();
        if (pageFragment.onBackPressed()) {
            return;
        }

        // If user enter PageActivity in portrait and leave in landscape,
        // we should hide the transition animation view to prevent bad animation.
        if (DimenUtil.isLandscape(this) || !hasTransitionAnimation) {
            wikiArticleCardView.setVisibility(View.GONE);
        } else {
            wikiArticleCardView.setVisibility(View.VISIBLE);
            pageFragmentView.setVisibility(View.GONE);
        }
        super.onBackPressed();
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
        loadPage(title, entry, TabPosition.CURRENT_TAB);
    }

    @Override
    public void onPageShowLinkPreview(@NonNull HistoryEntry entry) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                LinkPreviewDialog.newInstance(entry, null));
    }

    @Override
    public void onPageLoadMainPageInForegroundTab() {
        loadMainPage(TabPosition.EXISTING_TAB);
    }

    @Override
    public void onPageUpdateProgressBar(boolean visible) {
        updateProgressBar(visible);
    }

    @Override
    public void onPageStartSupportActionMode(@NonNull ActionMode.Callback callback) {
        startActionMode(callback);
    }

    @Override
    public void onPageHideSoftKeyboard() {
        hideSoftKeyboard();
    }

    @Override
    public void onPageAddToReadingList(@NonNull PageTitle title, @NonNull InvokeSource source) {
        showAddToListDialog(title, source);
    }

    @Override
    public void onPageMoveToReadingList(long sourceReadingListId, @NonNull PageTitle title, @NonNull InvokeSource source, boolean showDefaultList) {
        showMoveToListDialog(sourceReadingListId, title, source, showDefaultList);
    }

    @Override
    public void onPageWatchlistExpirySelect(@NonNull WatchlistExpiry expiry) {
        watchlistFunnel.logAddExpiry();
        pageFragment.updateWatchlist(expiry, false);
    }

    @Override
    public void onPageLoadError(@NonNull PageTitle title) {
        getSupportActionBar().setTitle(title.getDisplayText());
        removeTransitionAnimState();
    }

    @Override
    public void onPageLoadErrorBackPressed() {
        finish();
    }

    @Override
    public void onPageSetToolbarElevationEnabled(boolean enabled) {
        toolbarContainerView.setElevation(DimenUtil.dpToPx(enabled ? DimenUtil.getDimension(R.dimen.toolbar_default_elevation) : 0));
    }

    @Override
    public void onPageCloseActionMode() {
        Set<ActionMode> actionModesToFinish = new HashSet<>(currentActionModes);
        for (ActionMode mode : actionModesToFinish) {
            mode.finish();
        }
        currentActionModes.clear();
    }

    @Override
    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        loadPage(title, entry, inNewTab ? TabPosition.NEW_TAB_BACKGROUND : TabPosition.CURRENT_TAB);
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getUri());
        showCopySuccessMessage();
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        showAddToListDialog(title, LINK_PREVIEW_MENU);
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    private void copyLink(@NonNull String url) {
        ClipboardUtil.setPlainText(this, null, url);
    }

    private void showCopySuccessMessage() {
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    private void showOverflowMenu(@NonNull View anchor) {
        PageActionOverflowView overflowView = new PageActionOverflowView(this);
        overflowView.show(anchor, overflowCallback, pageFragment.getCurrentTab(), pageFragment.getModel().shouldLoadAsMobileWeb(),
                pageFragment.getModel().isWatched(), pageFragment.getModel().hasWatchlistExpiry());
    }

    private class OverflowCallback implements PageActionOverflowView.Callback {
        @Override
        public void forwardClick() {
            pageFragment.goForward();
        }

        @Override
        public void watchlistClick(boolean isWatched) {
            if (isWatched) {
                watchlistFunnel.logRemoveArticle();
            } else {
                watchlistFunnel.logAddArticle();
            }
            pageFragment.updateWatchlist(WatchlistExpiry.NEVER, isWatched);
        }

        @Override
        public void shareClick() {
            pageFragment.sharePageLink();
        }

        @Override
        public void newTabClick() {
            startActivity(newIntentForNewTab(PageActivity.this));
        }

        @Override
        public void feedClick() {
            goToMainTab(NavTab.EXPLORE);
        }

        @Override
        public void talkClick() {
            startActivity(TalkTopicsActivity.newIntent(PageActivity.this,
                    pageFragment.getTitle().pageTitleForTalkPage(), PAGE_ACTIVITY));
        }

        @Override
        public void editHistoryClick() {
            visitInExternalBrowser(PageActivity.this,
                    Uri.parse(pageFragment.getTitle().getWebApiUrl("action=history")));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.resetWikiSite();
        Prefs.storeTemporaryWikitext(null);
    }

    @Override
    public void onPause() {
        if (isCabOpen()) {
            // Explicitly close any current ActionMode (see T147191)
            onPageCloseActionMode();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(LANGUAGE_CODE_BUNDLE_KEY, app.getAppOrSystemLanguageCode());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == ACTIVITY_REQUEST_SETTINGS) {
            handleSettingsActivityResult(resultCode);
        } else if (newArticleLanguageSelected(requestCode, resultCode) || galleryPageSelected(requestCode, resultCode)) {
            toolbarContainerView.post(() -> handleIntent(data));
        } else if (galleryImageEdited(requestCode, resultCode)) {
            pageFragment.reloadFromBackstack();
        } else if (requestCode == Constants.ACTIVITY_REQUEST_BROWSE_TABS) {
            if (app.getTabCount() == 0 && resultCode != TabActivity.RESULT_NEW_TAB) {
                // They browsed the tabs and cleared all of them, without wanting to open a new tab.
                finish();
                return;
            }
            if (resultCode == TabActivity.RESULT_NEW_TAB) {
                loadMainPage(TabPosition.NEW_TAB_FOREGROUND);
                animateTabsButton();
            } else if (resultCode == TabActivity.RESULT_LOAD_FROM_BACKSTACK) {
                pageFragment.reloadFromBackstack();
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL
                && resultCode == RESULT_OK) {
            Prefs.setDescriptionEditTutorialEnabled(false);
            pageFragment.startDescriptionEditActivity(data.getStringExtra(DESCRIPTION_SELECTED_TEXT));
        } else if ((requestCode == Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT
                || requestCode == Constants.ACTIVITY_REQUEST_IMAGE_TAGS_EDIT
                || requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT)
                && (resultCode == RESULT_OK || resultCode == RESULT_OK_FROM_EDIT_SUCCESS)) {
            pageFragment.refreshPage();
            String editLanguage = StringUtils.defaultString(pageFragment.getLeadImageEditLang(), app.language().getAppLanguageCode());
            DescriptionEditActivity.Action action = (data != null && data.hasExtra(INTENT_EXTRA_ACTION)) ? (DescriptionEditActivity.Action) data.getSerializableExtra(INTENT_EXTRA_ACTION)
                    : (requestCode == Constants.ACTIVITY_REQUEST_IMAGE_TAGS_EDIT) ? ADD_IMAGE_TAGS : null;

            SuggestedEditsSnackbars.show(this, action, resultCode != RESULT_OK_FROM_EDIT_SUCCESS, editLanguage, requestCode != Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT, () -> {
                if (action == ADD_IMAGE_TAGS) {
                    startActivity(FilePageActivity.newIntent(this, pageFragment.getTitle()));
                } else if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
                    startActivity(GalleryActivity.newIntent(this,
                            pageFragment.getTitle(), pageFragment.getTitle().getPrefixedText(), pageFragment.getTitle().getWikiSite(), 0, GalleryFunnel.SOURCE_NON_LEAD_IMAGE));
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        if (unbinder != null) {
            unbinder.unbind();
        }
        disposables.clear();
        Prefs.setHasVisitedArticlePage(true);
        super.onDestroy();
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        if (!isCabOpen() && mode.getTag() == null) {
            modifyMenu(mode);
            ViewUtil.setCloseButtonInActionMode(pageFragment.requireContext(), mode);
            pageFragment.onActionModeShown(mode);
        }
        currentActionModes.add(mode);
    }

    private void modifyMenu(ActionMode mode) {
        Menu menu = mode.getMenu();
        ArrayList<MenuItem> menuItemsList = new ArrayList<>();

        for (int i = 0; i < menu.size(); i++) {
            String title = menu.getItem(i).getTitle().toString();
            if (!title.contains(getString(R.string.search_hint))
                    && !(title.contains(getString(R.string.menu_text_select_define)) && pageFragment.getShareHandler().shouldEnableWiktionaryDialog())) {
                menuItemsList.add(menu.getItem(i));
            }
        }

        menu.clear();
        mode.getMenuInflater().inflate(R.menu.menu_text_select, menu);
        for (MenuItem menuItem : menuItemsList) {
            menu.add(menuItem.getGroupId(), menuItem.getItemId(), Menu.NONE, menuItem.getTitle()).setIntent(menuItem.getIntent()).setIcon(menuItem.getIcon());
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        currentActionModes.remove(mode);
    }

    protected void clearActionBarTitle() {
        getSupportActionBar().setTitle("");
    }

    private void handleSettingsActivityResult(int resultCode) {
        if (resultCode == ACTIVITY_RESULT_LANGUAGE_CHANGED) {
            loadNewLanguageMainPage();
        }
    }

    private void loadNewLanguageMainPage() {
        Handler uiThread = new Handler(Looper.getMainLooper());
        uiThread.postDelayed(() -> {
            loadMainPage(TabPosition.EXISTING_TAB);
            WidgetProviderFeaturedPage.forceUpdateWidget(getApplicationContext());
        }, DateUtils.SECOND_IN_MILLIS);
    }

    private boolean newArticleLanguageSelected(int requestCode, int resultCode) {
        return requestCode == Constants.ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT;
    }

    private boolean galleryPageSelected(int requestCode, int resultCode) {
        return requestCode == Constants.ACTIVITY_REQUEST_GALLERY && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED;
    }

    private boolean galleryImageEdited(int requestCode, int resultCode) {
        return requestCode == Constants.ACTIVITY_REQUEST_GALLERY && (resultCode == GalleryActivity.ACTIVITY_RESULT_IMAGE_CAPTION_ADDED
                || resultCode == GalleryActivity.ACTIVITY_REQUEST_ADD_IMAGE_TAGS);
    }

    private void showDescriptionEditRevertDialog(@NonNull String qNumber) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_reverted_title)
                .setView(new DescriptionEditRevertHelpView(this, qNumber))
                .setPositiveButton(R.string.reverted_edit_dialog_ok_button_text, null)
                .create()
                .show();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private void maybeShowWatchlistTooltip() {
        if (!Prefs.isWatchlistPageOnboardingTooltipShown() && AccountUtil.isLoggedIn()
                && pageFragment.getHistoryEntry() != null
                && pageFragment.getHistoryEntry().getSource() != HistoryEntry.SOURCE_SUGGESTED_EDITS) {
            overflowButton.postDelayed(() -> {
                if (isDestroyed()) {
                    return;
                }
                watchlistFunnel.logShowTooltip();
                Prefs.setWatchlistPageOnboardingTooltipShown(true);
                FeedbackUtil.showTooltip(this, overflowButton, R.layout.view_watchlist_page_tooltip, -32, -8, false, false);
            }, 500);
        }
    }


    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ChangeTextSizeEvent) {
                if (pageFragment != null && pageFragment.getWebView() != null) {
                    pageFragment.updateFontSize();
                }
            } else if (event instanceof ArticleSavedOrDeletedEvent) {
                if (pageFragment == null || !pageFragment.isAdded() || pageFragment.getTitle() == null) {
                    return;
                }
                for (ReadingListPage page : ((ArticleSavedOrDeletedEvent) event).getPages()) {
                    if (page.getApiTitle().equals(pageFragment.getTitle().getPrefixedText())
                            && page.getWiki().languageCode().equals(pageFragment.getTitle().getWikiSite().languageCode())) {
                        pageFragment.updateBookmarkAndMenuOptionsFromDao();
                    }
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
