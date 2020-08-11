package org.wikipedia.page;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.LongPressHandler;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.ABTestSuggestedEditsSnackbarFunnel;
import org.wikipedia.analytics.FindInPageFunnel;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.analytics.PageScrollFunnel;
import org.wikipedia.analytics.TabFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.CommunicationBridge.CommunicationBridgeListener;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.dataclient.RestService;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient;
import org.wikipedia.dataclient.page.Protection;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.descriptions.DescriptionEditTutorialActivity;
import org.wikipedia.edit.EditHandler;
import org.wikipedia.feed.announcement.Announcement;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.UpdateHistoryTask;
import org.wikipedia.json.GsonUtil;
import org.wikipedia.language.LangLinksActivity;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.media.AvPlayer;
import org.wikipedia.media.DefaultAvPlayer;
import org.wikipedia.media.MediaPlayerImplementation;
import org.wikipedia.page.action.PageActionTab;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.leadimages.PageHeaderView;
import org.wikipedia.page.references.PageReferences;
import org.wikipedia.page.references.ReferenceDialog;
import org.wikipedia.page.shareafact.ShareHandler;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.search.SearchActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.suggestededits.PageSummaryForEdit;
import org.wikipedia.suggestededits.SuggestionsActivity;
import org.wikipedia.theme.ThemeChooserDialog;
import org.wikipedia.util.ActiveTimer;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GeoUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;
import org.wikipedia.views.WikiErrorView;
import org.wikipedia.wiktionary.WiktionaryDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_GALLERY;
import static org.wikipedia.Constants.InvokeSource.BOOKMARK_BUTTON;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTION_TAB;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTIVITY;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION;
import static org.wikipedia.descriptions.DescriptionEditTutorialActivity.DESCRIPTION_SELECTED_TEXT;
import static org.wikipedia.feed.announcement.Announcement.PLACEMENT_ARTICLE;
import static org.wikipedia.feed.announcement.AnnouncementClient.shouldShow;
import static org.wikipedia.page.PageActivity.ACTION_RESUME_READING;
import static org.wikipedia.page.PageCacher.loadIntoCache;
import static org.wikipedia.settings.Prefs.isDescriptionEditTutorialEnabled;
import static org.wikipedia.settings.Prefs.isLinkPreviewEnabled;
import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;
import static org.wikipedia.util.DimenUtil.getDensityScalar;
import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;
import static org.wikipedia.util.ResourceUtil.getThemedColor;
import static org.wikipedia.util.ThrowableUtil.isOffline;
import static org.wikipedia.util.UriUtil.decodeURL;
import static org.wikipedia.util.UriUtil.visitInExternalBrowser;

public class PageFragment extends Fragment implements BackPressedHandler, CommunicationBridgeListener,
        ThemeChooserDialog.Callback, ReferenceDialog.Callback, WiktionaryDialog.Callback {
    public interface Callback {
        void onPageDismissBottomSheet();
        void onPageLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry);
        void onPageInitWebView(@NonNull ObservableWebView v);
        void onPageShowLinkPreview(@NonNull HistoryEntry entry);
        void onPageLoadMainPageInForegroundTab();
        void onPageUpdateProgressBar(boolean visible);
        void onPageStartSupportActionMode(@NonNull ActionMode.Callback callback);
        void onPageHideSoftKeyboard();
        void onPageAddToReadingList(@NonNull PageTitle title, @NonNull InvokeSource source);
        void onPageMoveToReadingList(long sourceReadingListId, @NonNull PageTitle title, @NonNull InvokeSource source, boolean showDefaultList);
        void onPageRemoveFromReadingLists(@NonNull PageTitle title);
        void onPageLoadError(@NonNull PageTitle title);
        void onPageLoadErrorBackPressed();
        void onPageHideAllContent();
        void onPageSetToolbarFadeEnabled(boolean enabled);
        void onPageSetToolbarElevationEnabled(boolean enabled);
        void onPageCloseActionMode();
    }

    private boolean pageRefreshed;
    private boolean errorState = false;

    private static final int REFRESH_SPINNER_ADDITIONAL_OFFSET = (int) (16 * getDensityScalar());

    private PageFragmentLoadState pageFragmentLoadState;
    private PageViewModel model;

    @NonNull private TabFunnel tabFunnel = new TabFunnel();

    private PageScrollFunnel pageScrollFunnel;
    private LeadImagesHandler leadImagesHandler;
    private PageHeaderView pageHeaderView;
    private ObservableWebView webView;
    private CoordinatorLayout containerView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private WikiErrorView errorView;
    private PageActionTabLayout tabLayout;
    private ToCHandler tocHandler;
    private WebViewScrollTriggerListener scrollTriggerListener = new WebViewScrollTriggerListener();
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private EditHandler editHandler;
    private ShareHandler shareHandler;
    private CompositeDisposable disposables = new CompositeDisposable();
    private ActiveTimer activeTimer = new ActiveTimer();
    private PageReferences references;
    private long revision;
    @Nullable private AvPlayer avPlayer;
    @Nullable private AvCallback avCallback;
    @Nullable private List<Section> sections;

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
                    public void onAddRequest(boolean addToDefault) {
                        addToReadingList(getTitle(), BOOKMARK_BUTTON);
                    }

                    @Override
                    public void onMoveRequest(@Nullable ReadingListPage page) {
                        moveToReadingList(page.listId(), getTitle(), BOOKMARK_BUTTON, true);
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
                ReadingListBehaviorsUtil.INSTANCE.addToDefaultList(requireActivity(), getTitle(), BOOKMARK_BUTTON,
                        readingListId -> moveToReadingList(readingListId, getTitle(), BOOKMARK_BUTTON, false));
            }
        }

        @Override
        public void onSearchTabSelected() {
            openSearchActivity(PAGE_ACTION_TAB);
        }

        @Override
        public void onChooseLangTabSelected() {
            startLangLinksActivity();
        }

        @Override
        public void onFontAndThemeTabSelected() {
            showBottomSheet(new ThemeChooserDialog());
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

    @Override
    public PageViewModel getModel() {
        return model;
    }

    @Override
    public boolean isPreview() {
        return false;
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

    public ToCHandler getTocHandler() {
        return tocHandler;
    }

    public ViewGroup getContainerView() {
        return containerView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) requireActivity().getApplicationContext();
        model = new PageViewModel();
        pageFragmentLoadState = new PageFragmentLoadState();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);
        pageHeaderView = rootView.findViewById(R.id.page_header_view);

        webView = rootView.findViewById(R.id.page_web_view);
        initWebViewListeners();

        containerView = rootView.findViewById(R.id.page_contents_container);
        refreshView = rootView.findViewById(R.id.page_refresh_container);
        refreshView.setColorSchemeResources(getThemedAttributeId(requireContext(), R.attr.colorAccent));
        refreshView.setScrollableChild(webView);
        refreshView.setOnRefreshListener(pageRefreshListener);

        tabLayout = rootView.findViewById(R.id.page_actions_tab_layout);
        tabLayout.setPageActionTabsCallback(pageActionTabsCallback);

        errorView = rootView.findViewById(R.id.page_error);

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
        tocHandler.log();
        shareHandler.dispose();
        leadImagesHandler.dispose();
        disposables.clear();
        webView.clearAllListeners();
        ((ViewGroup) webView.getParent()).removeView(webView);
        webView = null;
        Prefs.setSuggestedEditsHighestPriorityEnabled(false);
        super.onDestroyView();
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

        bridge = new CommunicationBridge(this);
        setupMessageHandlers();

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

        tocHandler = new ToCHandler(this, requireActivity().getWindow().getDecorView().findViewById(R.id.navigation_drawer),
                requireActivity().getWindow().getDecorView().findViewById(R.id.page_scroller_button), bridge);

        // TODO: initialize View references in onCreateView().
        leadImagesHandler = new LeadImagesHandler(this, webView, pageHeaderView);

        shareHandler = new ShareHandler(this, bridge);

        if (callback() != null) {
            new LongPressHandler(webView, HistoryEntry.SOURCE_INTERNAL_LINK, new PageContainerLongPressHandler(this));
        }

        pageFragmentLoadState.setUp(model, this, webView, bridge, leadImagesHandler, getCurrentTab());

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

    void updateInsets(@NonNull WindowInsetsCompat insets) {
        int swipeOffset = getContentTopOffsetPx(requireActivity()) + insets.getSystemWindowInsetTop() + REFRESH_SPINNER_ADDITIONAL_OFFSET;
        refreshView.setProgressViewOffset(false, -swipeOffset, swipeOffset);
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
        webView.addOnContentHeightChangedListener(scrollTriggerListener);
        webView.setWebViewClient(new OkHttpWebViewClient() {
            @NonNull @Override public PageViewModel getModel() {
                return model;
            }

            @NonNull @Override public LinkHandler getLinkHandler() {
                return linkHandler;
            }

            public void onPageFinished(WebView view, String url) {
                bridge.evaluateImmediate("(function() { return (typeof pcs !== 'undefined'); })();", pcsExists -> {
                    if (!isAdded()) {
                        return;
                    }
                    // TODO: This is a bit of a hack: If PCS does not exist in the current page, then
                    // it's implied that this page was loaded via Mobile Web (e.g. the Main Page) and
                    // doesn't support PCS, meaning that we will never receive the `setup` event that
                    // tells us the page is finished loading. In such a case, we must infer that the
                    // page has now loaded and trigger the remaining logic ourselves.
                    if (!"true".equals(pcsExists)) {
                        onPageSetupEvent();
                        bridge.onPcsReady();
                        bridge.execute(JavaScriptActionHandler.mobileWebChromeShim());
                    }
                });
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                onPageLoadError(new RuntimeException(description));
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (!request.getUrl().toString().contains(RestService.PAGE_HTML_ENDPOINT)) {
                    // If the request is anything except the main mobile-html content request, then
                    // don't worry about any errors and let the WebView deal with it.
                    return;
                }
                onPageLoadError(new HttpStatusException(errorResponse.getStatusCode(), request.getUrl().toString(), UriUtil.decodeURL(errorResponse.getReasonPhrase())));
            }
        });
    }

    public void onPageMetadataLoaded() {
        if (model.getPage() == null) {
            return;
        }
        editHandler.setPage(model.getPage());
        refreshView.setEnabled(true);
        refreshView.setRefreshing(false);
        requireActivity().invalidateOptionsMenu();
        initPageScrollFunnel();

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

        if (!errorState) {
            editHandler.setPage(model.getPage());
            webView.setVisibility(View.VISIBLE);
        }

        checkAndShowBookmarkOnboarding();
        maybeShowAnnouncement();

        bridge.onMetadataReady();
        // Explicitly set the top margin (even though it might have already been set in the setup
        // handler), since the page metadata might have altered the lead image display state.
        bridge.execute(JavaScriptActionHandler.setTopMargin(leadImagesHandler.getTopMargin()));
        bridge.execute(JavaScriptActionHandler.setFooter(model));
    }

    private void onPageSetupEvent() {
        if (!isAdded()) {
            return;
        }

        updateProgressBar(false);
        webView.setVisibility(View.VISIBLE);
        app.getSessionFunnel().leadSectionFetchEnd();

        bridge.evaluate(JavaScriptActionHandler.getRevision(), revision -> {
            if (!isAdded()) {
                return;
            }
            try {
                this.revision = Long.parseLong(revision.replace("\"", ""));
            } catch (Exception e) {
                L.e(e);
            }
        });

        bridge.evaluate(JavaScriptActionHandler.getSections(), value -> {
            if (!isAdded() || model.getPage() == null) {
                return;
            }
            Section[] secArray = GsonUtil.getDefaultGson().fromJson(value, Section[].class);
            if (secArray != null) {
                sections = new ArrayList<>(Arrays.asList(secArray));
                sections.add(0, new Section(0, 0, model.getTitle().getDisplayText(), model.getTitle().getDisplayText(), ""));
                model.getPage().setSections(sections);
            }
            tocHandler.setupToC(model.getPage(), model.getTitle().getWikiSite());
            tocHandler.setEnabled(true);
        });

        bridge.evaluate(JavaScriptActionHandler.getProtection(), value -> {
            if (!isAdded() || model.getPage() == null) {
                return;
            }
            Protection protection = GsonUtil.getDefaultGson().fromJson(value, Protection.class);
            model.getPage().getPageProperties().setProtection(protection);
            bridge.execute(JavaScriptActionHandler.setUpEditButtons(true, !model.getPage().getPageProperties().canEdit()));
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
            visitInExternalBrowser(requireActivity(), Uri.parse(title.getUri()));
            return;
        }
        dismissBottomSheet();
        HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
        if (model.getTitle() != null) {
            historyEntry.setReferrer(model.getTitle().getUri());
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // if the screen orientation changes, then re-layout the lead image container,
        // but only if we've finished fetching the page.
        if (!bridge.isLoading() && !errorState) {
            pageFragmentLoadState.onConfigurationChanged();
        }
    }

    public Tab getCurrentTab() {
        return app.getTabList().get(app.getTabList().size() - 1);
    }

    private void setCurrentTabAndReset(int position) {
        // move the selected tab to the bottom of the list, and navigate to it!
        // (but only if it's a different tab than the one currently in view!
        if (position < app.getTabList().size() - 1) {
            Tab tab = app.getTabList().remove(position);
            app.getTabList().add(tab);
            pageFragmentLoadState.setTab(tab);
        }
        if (app.getTabCount() > 0) {
            app.getTabList().get(app.getTabList().size() - 1).squashBackstack();
            pageFragmentLoadState.loadFromBackStack();
        }
    }

    public void openInNewBackgroundTab(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        if (app.getTabCount() == 0) {
            openInNewTab(title, entry, getForegroundTabPosition());
            pageFragmentLoadState.loadFromBackStack();
        } else {
            openInNewTab(title, entry, getBackgroundTabPosition());
            ((PageActivity) requireActivity()).animateTabsButton();
        }
    }

    public void openInNewForegroundTab(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        openInNewTab(title, entry, getForegroundTabPosition());
        pageFragmentLoadState.loadFromBackStack();
    }

    private void openInNewTab(@NonNull PageTitle title, @NonNull HistoryEntry entry, int position) {
        int selectedTabPosition = -1;
        for (Tab tab : app.getTabList()) {
            if (tab.getBackStackPositionTitle() != null && tab.getBackStackPositionTitle().equals(title)) {
                selectedTabPosition = app.getTabList().indexOf(tab);
                break;
            }
        }
        if (selectedTabPosition >= 0) {
            setCurrentTabAndReset(selectedTabPosition);
            return;
        }

        tabFunnel.logOpenInNew(app.getTabList().size());

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
            pageFragmentLoadState.setTab(getCurrentTab());
            getCurrentTab().getBackStack().add(new PageBackStackItem(title, entry));
        }
    }

    public void openFromExistingTab(@NonNull PageTitle title, @NonNull HistoryEntry entry) {
        // find the tab in which this title appears...
        int selectedTabPosition = -1;
        for (Tab tab : app.getTabList()) {
            if (tab.getBackStackPositionTitle() != null && tab.getBackStackPositionTitle().equals(title)) {
                selectedTabPosition = app.getTabList().indexOf(tab);
                break;
            }
        }
        if (selectedTabPosition == -1) {
            loadPage(title, entry, true, true);
            return;
        }
        setCurrentTabAndReset(selectedTabPosition);
    }

    public void loadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean pushBackStack, boolean squashBackstack) {
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
        if (squashBackstack) {
            if (app.getTabCount() > 0) {
                app.getTabList().get(app.getTabList().size() - 1).clearBackstack();
            }
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

        tocHandler.setEnabled(false);
        errorState = false;
        errorView.setVisibility(View.GONE);

        model.setTitle(title);
        model.setTitleOriginal(title);
        model.setCurEntry(entry);
        model.setReadingListPage(null);
        model.setForceNetwork(isRefresh);

        webView.setVisibility(View.VISIBLE);
        tabLayout.setVisibility(View.VISIBLE);

        tabLayout.enableAllTabs();

        updateProgressBar(true);

        pageRefreshed = isRefresh;
        references = null;
        revision = 0;

        closePageScrollFunnel();
        pageFragmentLoadState.load(pushBackStack);
        scrollTriggerListener.setStagedScrollY(stagedScrollY);
        updateBookmarkAndMenuOptions();
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
                .doAfterTerminate(() -> {
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
            FeedbackUtil.showMessage(requireActivity(), R.string.edit_saved_successfully);
            // and reload the page...
            loadPage(model.getTitleOriginal(), model.getCurEntry(), false, false);
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL
                && resultCode == RESULT_OK) {
            Prefs.setDescriptionEditTutorialEnabled(false);
            startDescriptionEditActivity(data.getStringExtra(DESCRIPTION_SELECTED_TEXT));
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT
                && resultCode == RESULT_OK) {
            refreshPage();
            ABTestSuggestedEditsSnackbarFunnel abTestFunnel = new ABTestSuggestedEditsSnackbarFunnel();
            Snackbar snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.description_edit_success_saved_snackbar), FeedbackUtil.LENGTH_DEFAULT);
            if (abTestFunnel.shouldSeeSnackbarAction()) {
                snackbar.setAction(R.string.suggested_edits_tasks_onboarding_get_started, view -> startSuggestionsActivity(ADD_DESCRIPTION));
            }
            snackbar.show();
            abTestFunnel.logSnackbarShown();
        }
    }

    public void sharePageLink() {
        if (getPage() != null) {
            ShareUtil.shareText(requireActivity(), getPage().getTitle());
        }
    }

    public View getHeaderView() {
        return pageHeaderView;
    }

    private void showFindReferenceInPage(@NonNull String referenceAnchor, @NonNull List<String> backLinksList, @NonNull String referenceText) {
        if (model.getPage() == null) {
            return;
        }
        startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuItem menuItem = menu.add(R.string.menu_page_find_in_page);
                menuItem.setActionProvider(new FindReferenceInPageActionProvider(requireContext(), referenceAnchor, referenceText, backLinksList));
                menuItem.expandActionView();
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTag("actionModeFindReferenceInPage");
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) { }
        });
    }

    void showFindInPage() {
        if (model.getPage() == null) {
            return;
        }
        final FindInPageFunnel funnel = new FindInPageFunnel(app, model.getTitle().getWikiSite(),
                model.getPage().getPageProperties().getPageId());
        final FindInWebPageActionProvider findInPageActionProvider
                = new FindInWebPageActionProvider(this, funnel);

        startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuItem menuItem = menu.add(R.string.menu_page_find_in_page);
                menuItem.setActionProvider(findInPageActionProvider);
                menuItem.expandActionView();
                setToolbarElevationEnabled(false);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTag("actionModeFindInPage");
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
                funnel.setPageHeight(webView.getContentHeight());
                funnel.logDone();
                webView.clearMatches();
                hideSoftKeyboard();
                setToolbarElevationEnabled(true);
            }
        });
    }

    /**
     * Scroll to a specific section in the WebView.
     * @param sectionAnchor Anchor link of the section to scroll to.
     */
    public void scrollToSection(@NonNull String sectionAnchor) {
        if (!isAdded()) {
            return;
        }
        bridge.execute(JavaScriptActionHandler.prepareToScrollTo(sectionAnchor, false));
    }

    public void onPageLoadError(@NonNull Throwable caught) {
        if (!isAdded()) {
            return;
        }
        updateProgressBar(false);
        refreshView.setRefreshing(false);

        if (pageRefreshed) {
            pageRefreshed = false;
        }

        hidePageContent();
        bridge.onMetadataReady();

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
        if (bridge.isLoading()) {
            refreshView.setRefreshing(false);
            return;
        }

        errorView.setVisibility(View.GONE);

        tabLayout.enableAllTabs();
        errorState = false;

        model.setCurEntry(new HistoryEntry(model.getTitle(), HistoryEntry.SOURCE_HISTORY));
        loadPage(model.getTitle(), model.getCurEntry(), false, stagedScrollY, app.isOnline());
    }

    boolean isLoading() {
        return bridge.isLoading();
    }

    private void setBookmarkIconForPageSavedState(boolean pageSaved) {
        View bookmarkTab = tabLayout.getChildAt(PageActionTab.ADD_TO_READING_LIST.code());
        if (bookmarkTab != null) {
            ((MaterialTextView) bookmarkTab).setCompoundDrawablesWithIntrinsicBounds(null,
                    AppCompatResources.getDrawable(requireContext(), pageSaved
                            ? R.drawable.ic_bookmark_white_24dp
                            : R.drawable.ic_bookmark_border_white_24dp), null, null);
            bookmarkTab.setEnabled(!model.shouldLoadAsMobileWeb());
        }
    }

    protected void clearActivityActionBarTitle() {
        FragmentActivity currentActivity = requireActivity();
        if (currentActivity instanceof PageActivity) {
            ((PageActivity) currentActivity).clearActionBarTitle();
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

    @SuppressWarnings("checkstyle:methodlength")
    private void setupMessageHandlers() {
        linkHandler = new LinkHandler(requireActivity()) {
            @Override public void onPageLinkClicked(@NonNull String anchor, @NonNull String linkText) {
                dismissBottomSheet();
                bridge.execute(JavaScriptActionHandler.prepareToScrollTo(anchor, true));
            }

            @Override public void onInternalLinkClicked(@NonNull PageTitle title) {
                handleInternalLink(title);
            }

            @Override public void onMediaLinkClicked(@NonNull PageTitle title) {
                startGalleryActivity(title.getPrefixedText());
            }

            @Override public WikiSite getWikiSite() {
                return model.getTitle().getWikiSite();
            }
        };
        bridge.addListener("link", linkHandler);

        bridge.addListener("setup", (String messageType, JsonObject messagePayload) -> {
            onPageSetupEvent();
        });
        bridge.addListener("final_setup", (String messageType, JsonObject messagePayload) -> {
            if (!isAdded()) {
                return;
            }
            bridge.onPcsReady();
        });
        bridge.addListener("reference", (String messageType, JsonObject messagePayload) -> {
            if (!isAdded()) {
                return;
            }

            references = GsonUtil.getDefaultGson().fromJson(messagePayload, PageReferences.class);

            if (!references.getReferencesGroup().isEmpty()) {
                showBottomSheet(new ReferenceDialog());
            }
        });
        bridge.addListener("back_link", (String messageType, JsonObject payload) -> {
            JsonArray backLinks = payload.getAsJsonArray("backLinks");
            if (backLinks.size() > 0) {
                List<String> backLinksList = new ArrayList<>();
                for (int i = 0; i < backLinks.size(); i++) {
                    backLinksList.add(backLinks.get(i).getAsJsonObject().get("id").getAsString());
                }
                showFindReferenceInPage(StringUtils.defaultString(payload.get("referenceId").getAsString()),
                        backLinksList, StringUtils.defaultString(payload.get("referenceText").getAsString()));
            }
        });
        bridge.addListener("scroll_to_anchor", (String messageType, JsonObject payload) -> {
            int diffY = payload.getAsJsonObject("rect").has("y")
                    ? DimenUtil.roundedDpToPx(payload.getAsJsonObject("rect").get("y").getAsFloat())
                    : DimenUtil.roundedDpToPx(payload.getAsJsonObject("rect").get("top").getAsFloat());
            final int offsetFraction = 3;
            webView.setScrollY(webView.getScrollY() + diffY - webView.getHeight() / offsetFraction);
        });
        bridge.addListener("image", (String messageType, JsonObject messagePayload) -> {
            linkHandler.onUrlClick(decodeURL(messagePayload.get("href").getAsString()),
                    messagePayload.has("title") ? messagePayload.get("title").getAsString() : null, "");
        });
        bridge.addListener("media", (String messageType, JsonObject messagePayload) -> {
            linkHandler.onUrlClick(decodeURL(messagePayload.get("href").getAsString()),
                    messagePayload.has("title") ? messagePayload.get("title").getAsString() : null, "");
        });
        bridge.addListener("pronunciation", (String messageType, JsonObject messagePayload) -> {
            if (avPlayer == null) {
                avPlayer = new DefaultAvPlayer(new MediaPlayerImplementation());
                avPlayer.init();
            }
            if (avCallback == null) {
                avCallback = new AvCallback();
            }
            if (!avPlayer.isPlaying() && messagePayload.has("url")) {
                updateProgressBar(true);
                avPlayer.play(UriUtil.resolveProtocolRelativeUrl(messagePayload.get("url").getAsString()), avCallback, avCallback);
            } else {
                updateProgressBar(false);
                avPlayer.stop();
            }
        });
        bridge.addListener("footer_item", (String messageType, JsonObject messagePayload) -> {
            String itemType = messagePayload.get("itemType").getAsString();
            if ("talkPage".equals(itemType) && model.getTitle() != null) {
                PageTitle talkPageTitle = new PageTitle("Talk", model.getTitle().getPrefixedText(), model.getTitle().getWikiSite());
                visitInExternalBrowser(requireContext(), Uri.parse(talkPageTitle.getUri()));
            } else if ("languages".equals(itemType)) {
                startLangLinksActivity();
            } else if ("lastEdited".equals(itemType) && model.getTitle() != null) {
                visitInExternalBrowser(requireContext(), Uri.parse(model.getTitle().getUriForAction("history")));
            } else if ("coordinate".equals(itemType) && model.getPage() != null && model.getPage().getPageProperties().getGeo() != null) {
                GeoUtil.sendGeoIntent(requireActivity(), model.getPage().getPageProperties().getGeo(), model.getPage().getDisplayTitle());
            } else if ("disambiguation".equals(itemType)) {
                // TODO
                // messagePayload contains an array of URLs called "payload".
            }
        });
        bridge.addListener("read_more_titles_retrieved", (String messageType, JsonObject messagePayload) -> {
            // TODO: do something with this.
        });
        bridge.addListener("view_license", (String messageType, JsonObject messagePayload) -> {
            visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.cc_by_sa_3_url)));
        });
        bridge.addListener("view_in_browser", (String messageType, JsonObject messagePayload) -> {
            if (model.getTitle() != null) {
                visitInExternalBrowser(requireContext(), Uri.parse(model.getTitle().getUri()));
            }
        });
    }

    public void verifyBeforeEditingDescription(@Nullable String text) {
        if (getPage() != null && getPage().getPageProperties().canEdit()) {
            if (!AccountUtil.isLoggedIn() && Prefs.getTotalAnonDescriptionsEdited() >= getResources().getInteger(R.integer.description_max_anon_edits)) {
                new AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.description_edit_anon_limit)
                        .setPositiveButton(R.string.page_editing_login, (DialogInterface dialogInterface, int i) ->
                                startActivity(LoginActivity.newIntent(requireContext(), LoginFunnel.SOURCE_EDIT)))
                        .setNegativeButton(R.string.description_edit_login_cancel_button_text, null)
                        .show();
            } else {
                startDescriptionEditActivity(text);
            }
        } else {
            getEditHandler().showUneditableDialog();
        }
    }

    private void startDescriptionEditActivity(@Nullable String text) {
        if (isDescriptionEditTutorialEnabled()) {
            startActivityForResult(DescriptionEditTutorialActivity.newIntent(requireContext(), text),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL);
        } else {
            PageSummaryForEdit sourceSummary = new PageSummaryForEdit(getTitle().getPrefixedText(), getTitle().getWikiSite().languageCode(), getTitle(),
                    getTitle().getDisplayText(), getTitle().getDescription(), getTitle().getThumbUrl());
            startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), getTitle(), text, sourceSummary, null, ADD_DESCRIPTION, PAGE_ACTIVITY),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT);
        }
    }

    private void startGalleryActivity(@NonNull String fileName) {
        if (app.isOnline()) {
            requireActivity().startActivityForResult(GalleryActivity.newIntent(requireActivity(),
                    model.getTitle(), fileName,
                    model.getTitle().getWikiSite(), getRevision(), GalleryFunnel.SOURCE_NON_LEAD_IMAGE), ACTIVITY_REQUEST_GALLERY);
        } else {
            Snackbar snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.gallery_not_available_offline_snackbar), FeedbackUtil.LENGTH_DEFAULT);
            snackbar.setAction(R.string.gallery_not_available_offline_snackbar_dismiss, view -> snackbar.dismiss());
            snackbar.show();
        }
    }

    public void startSuggestionsActivity(@NonNull DescriptionEditActivity.Action action) {
        startActivity(SuggestionsActivity.newIntent(requireActivity(), action));
    }

    /**
     * Convenience method for hiding all the content of a page.
     */
    private void hidePageContent() {
        leadImagesHandler.hide();
        bridge.loadBlankPage();
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
        if (pageFragmentLoadState.goBack()) {
            return true;
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

    public void showBottomSheet(@NonNull BottomSheetDialogFragment dialog) {
        bottomSheetPresenter.show(getChildFragmentManager(), dialog);
    }

    private void dismissBottomSheet() {
        bottomSheetPresenter.dismiss(getChildFragmentManager());
        Callback callback = callback();
        if (callback != null) {
            callback.onPageDismissBottomSheet();
        }
    }

    @Override
    public LinkHandler getLinkHandler() {
        return linkHandler;
    }

    @Override
    public List<PageReferences.Reference> getReferencesGroup() {
        return references != null ? references.getReferencesGroup() : null;
    }

    @Override
    public int getSelectedReferenceIndex() {
        return references.getSelectedIndex();
    }

    @Override
    public void onToggleDimImages() {
        requireActivity().recreate();
    }

    @Override
    public void onCancel() {
    }

    @Override
    public void wiktionaryShowDialogForTerm(@NonNull String term) {
        shareHandler.showWiktionaryDefinition(term);
    }

    public int getToolbarMargin() {
        return ((PageActivity) requireActivity()).toolbarContainerView.getHeight();
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

    private void updateProgressBar(boolean visible) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageUpdateProgressBar(visible);
        }
    }

    public void startSupportActionMode(@NonNull ActionMode.Callback actionModeCallback) {
        if (callback() != null) {
            callback().onPageStartSupportActionMode(actionModeCallback);
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

    public void addToReadingList(@NonNull PageTitle title, @NonNull InvokeSource source) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageAddToReadingList(title, source);
        }
    }

    public void moveToReadingList(long sourceReadingListId, @NonNull PageTitle title, @NonNull InvokeSource source, boolean showDefaultList) {
        Callback callback = callback();
        if (callback != null) {
            callback.onPageMoveToReadingList(sourceReadingListId, title, source, showDefaultList);
        }
    }

    private void startLangLinksActivity() {
        Intent langIntent = new Intent();
        langIntent.setClass(requireActivity(), LangLinksActivity.class);
        langIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
        langIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, model.getTitle());
        requireActivity().startActivityForResult(langIntent, Constants.ACTIVITY_REQUEST_LANGLINKS);
    }

    public void openSearchActivity(@NonNull InvokeSource source) {
        Intent intent = SearchActivity.newIntent(requireContext(), source, null);
        requireActivity().startActivity(intent);
    }

    public long getRevision() {
        return revision;
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
                updateProgressBar(false);
            }
        }
        @Override
        public void onError() {
            if (avPlayer != null) {
                avPlayer.stop();
                updateProgressBar(false);
            }
        }
    }

    private class WebViewScrollTriggerListener implements ObservableWebView.OnContentHeightChangedListener {
        private int stagedScrollY;

        void setStagedScrollY(int stagedScrollY) {
            this.stagedScrollY = stagedScrollY;
        }

        @Override
        public void onContentHeightChanged(int contentHeight) {
            if (stagedScrollY > 0 && (contentHeight * getDensityScalar() - webView.getHeight()) > stagedScrollY) {
                webView.setScrollY(stagedScrollY);
                stagedScrollY = 0;
            }
        }
    }

    @Nullable
    public Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    @Nullable String getLeadImageEditLang() {
        return leadImagesHandler.getCallToActionEditLang();
    }

    void openImageInGallery(@NonNull String language) {
        leadImagesHandler.openImageInGallery(language);
    }

    private void maybeShowAnnouncement() {
        if (Prefs.hasVisitedArticlePage()) {
            disposables.add(ServiceFactory.getRest(getTitle().getWikiSite()).getAnnouncements()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(list -> {
                        String country = GeoUtil.getGeoIPCountry();
                        Date now = new Date();
                        for (Announcement announcement : list.items()) {
                            if (shouldShow(announcement, country, now)
                                    && announcement.placement().equals(PLACEMENT_ARTICLE)
                                    && !Prefs.getAnnouncementShownDialogs().contains(announcement.id())) {
                                AnnouncementDialog dialog = new AnnouncementDialog(requireActivity(), announcement);
                                dialog.setCancelable(false);
                                dialog.show();
                                break;
                            }
                        }
                    }, L::d));
        }
    }

    private class FindReferenceInPageActionProvider extends ActionProvider implements View.OnClickListener {
        private TextView referenceLabel, referenceCount;
        private View findInPageNext, findInPagePrev, findInPageClose;
        private String referenceAnchor, referenceText;
        private List<String> backLinksList;
        private int currentPos;

        FindReferenceInPageActionProvider(@NonNull Context context, @NonNull String referenceAnchor,
                                          @NonNull String referenceText, @NonNull List<String> backLinksList) {
            super(context);
            this.referenceAnchor = referenceAnchor;
            this.referenceText = referenceText;
            this.backLinksList = backLinksList;
        }

        @Override
        public View onCreateActionView() {
            View view = View.inflate(requireContext(), R.layout.group_find_references_in_page, null);
            referenceLabel = view.findViewById(R.id.reference_label);
            referenceCount = view.findViewById(R.id.reference_count);
            findInPagePrev = view.findViewById(R.id.find_in_page_prev);
            findInPageNext = view.findViewById(R.id.find_in_page_next);
            findInPageClose = view.findViewById(R.id.close_button);
            findInPagePrev.setOnClickListener(this);
            findInPageNext.setOnClickListener(this);
            referenceLabel.setOnClickListener(this);
            findInPageClose.setOnClickListener(this);

            referenceLabel.setText(getString(R.string.reference_list_title) + " " + StringUtils.defaultString(referenceText));

            if (!backLinksList.isEmpty()) {
                scrollTo(0);
            }
            return view;
        }

        @Override
        public boolean overridesItemVisibility() {
            return true;
        }

        @Override
        public void onClick(View v) {
            if (v.equals(findInPagePrev)) {
                if (!backLinksList.isEmpty()) {
                    currentPos = --currentPos < 0 ? backLinksList.size() - 1 : currentPos;
                    scrollTo(currentPos);
                }
            } else if (v.equals(findInPageNext)) {
                if (!backLinksList.isEmpty()) {
                    currentPos = ++currentPos >= backLinksList.size() ? 0 : currentPos;
                    scrollTo(currentPos);
                }
            } else if (v.equals(referenceLabel)) {
                bridge.execute(JavaScriptActionHandler.scrollToAnchor(referenceAnchor));
                callback().onPageCloseActionMode();
            } else if (v.equals(findInPageClose)) {
                callback().onPageCloseActionMode();
            }
        }

        private void scrollTo(int position) {
            referenceCount.setText(getString(R.string.find_in_page_result, position + 1, backLinksList.isEmpty() ? 0 : backLinksList.size()));
            bridge.execute(JavaScriptActionHandler.prepareToScrollTo(backLinksList.get(position), true));
        }
    }

}
