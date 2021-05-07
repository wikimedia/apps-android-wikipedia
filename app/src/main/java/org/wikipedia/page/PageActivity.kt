package org.wikipedia.page

import android.app.SearchManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.FixedDrawerLayout
import androidx.preference.PreferenceManager
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.GalleryFunnel
import org.wikipedia.analytics.IntentFunnel
import org.wikipedia.analytics.LinkPreviewFunnel
import org.wikipedia.analytics.WatchlistFunnel
import org.wikipedia.auth.AccountUtil
import org.wikipedia.auth.AccountUtil.isLoggedIn
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.commons.FilePageActivity.Companion.newIntent
import org.wikipedia.databinding.ActivityPageBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditRevertHelpView
import org.wikipedia.descriptions.DescriptionEditSuccessActivity
import org.wikipedia.descriptions.DescriptionEditTutorialActivity
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.events.ChangeTextSizeEvent
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.LangLinksActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.PageActivity
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.linkpreview.LinkPreviewDialog.Companion.newInstance
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.page.tabs.TabActivity.Companion.captureFirstTabBitmap
import org.wikipedia.page.tabs.TabActivity.Companion.newIntentFromPageActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.search.SearchActivity.Companion.newIntent
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.SiteInfoClient.getMainPageForLang
import org.wikipedia.suggestededits.SuggestedEditsSnackbars.OpenPageListener
import org.wikipedia.suggestededits.SuggestedEditsSnackbars.show
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.talk.TalkTopicsActivity.Companion.newIntent
import org.wikipedia.util.ClipboardUtil.setPlainText
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DeviceUtil.hideSoftKeyboard
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.DimenUtil.dpToPx
import org.wikipedia.util.DimenUtil.getDimension
import org.wikipedia.util.DimenUtil.isLandscape
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.FeedbackUtil.showMessage
import org.wikipedia.util.FeedbackUtil.showTooltip
import org.wikipedia.util.ReleaseUtil.isProdRelease
import org.wikipedia.util.ResourceUtil.getThemedColor
import org.wikipedia.util.ShareUtil.shareText
import org.wikipedia.util.ThrowableUtil.getInnermostThrowable
import org.wikipedia.util.UriUtil.visitInExternalBrowser
import org.wikipedia.views.*
import org.wikipedia.views.ViewUtil.setCloseButtonInActionMode
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.widgets.WidgetProviderFeaturedPage.Companion.forceUpdateWidget
import java.util.*

class PageActivity : BaseActivity(), PageFragment.Callback, LinkPreviewDialog.Callback,
    FrameLayoutNavMenuTriggerer.Callback {

    enum class TabPosition {
        CURRENT_TAB, CURRENT_TAB_SQUASH, NEW_TAB_BACKGROUND, NEW_TAB_FOREGROUND, EXISTING_TAB
    }

    private lateinit var binding: ActivityPageBinding
    private lateinit var toolbarHideHandler: ViewHideHandler
    private lateinit var pageFragment: PageFragment
    private var app = applicationContext as WikipediaApp
    private var hasTransitionAnimation = false
    private var wasTransitionShown = false
    private val currentActionModes = mutableSetOf<ActionMode>()
    private val disposables = CompositeDisposable()
    private val overflowCallback = OverflowCallback()
    private val watchlistFunnel = WatchlistFunnel()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val listDialogDismissListener = DialogInterface.OnDismissListener { pageFragment.updateBookmarkAndMenuOptionsFromDao() }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        binding = ActivityPageBinding.inflate(layoutInflater)

        try {
            setContentView(binding.root)
        } catch (e: Exception) {
            if (!e.message.isNullOrEmpty() && e.message!!.toLowerCase(Locale.getDefault()).contains("webview")
                || !getInnermostThrowable(e).message.isNullOrEmpty() && getInnermostThrowable(e).message!!.toLowerCase(Locale.getDefault()).contains("webview")) {
                // If the system failed to inflate our activity because of the WebView (which could
                // be one of several types of exceptions), it likely means that the system WebView
                // is in the process of being updated. In this case, show the user a message and
                // bail immediately.
                Toast.makeText(app, R.string.error_webview_updating, Toast.LENGTH_LONG).show()
                finish()
                return
            }
            throw e
        }

        disposables.add(app.bus.subscribe(EventBusConsumer()))
        updateProgressBar(false)
        pageFragment = supportFragmentManager.findFragmentById(R.id.page_fragment) as PageFragment

        // Toolbar setup
        setSupportActionBar(binding.pageToolbar)
        clearActionBarTitle()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.pageToolbarButtonSearch.setOnClickListener {
            startActivity(newIntent(this@PageActivity, InvokeSource.TOOLBAR, null))
        }
        binding.pageToolbarButtonTabs.setColor(getThemedColor(this, R.attr.material_theme_de_emphasised_color))
        binding.pageToolbarButtonTabs.updateTabCount(false)
        binding.pageToolbarButtonTabs.setOnClickListener {
            captureFirstTabBitmap(pageFragment.containerView)
            startActivityForResult(newIntentFromPageActivity(this), Constants.ACTIVITY_REQUEST_BROWSE_TABS)
        }
        toolbarHideHandler = ViewHideHandler(binding.pageToolbarContainer, null, Gravity.TOP)
        setButtonLongPressToast(binding.pageToolbarButtonTabs, binding.pageToolbarButtonShowOverflowMenu)
        binding.pageToolbarButtonShowOverflowMenu.setOnClickListener {
            showOverflowMenu(it)
        }

        // Navigation setup
        binding.navigationDrawer.setScrimColor(Color.TRANSPARENT)
        binding.containerWithNavTrigger.callback = this
        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationDrawer) { _, insets ->
            binding.pageToolbarContainer.setPadding(0, insets.systemWindowInsetTop, 0, 0)
            pageFragment.updateInsets(insets)
            insets
        }

        // WikiArticleCard setup
        hasTransitionAnimation = intent.getBooleanExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, false)
        binding.wikiArticleCardView.visibility = if (hasTransitionAnimation) View.VISIBLE else View.GONE

        // Search setup
        var languageChanged = false
        savedInstanceState?.let {
            if (it.getBoolean("isSearching")) {
                startActivity(SearchActivity.newIntent(this, InvokeSource.TOOLBAR, null))
            }
            val language = savedInstanceState.getString(LANGUAGE_CODE_BUNDLE_KEY)
            languageChanged = app.appOrSystemLanguageCode != language
        }
        if (languageChanged) {
            app.resetWikiSite()
            loadMainPage(TabPosition.EXISTING_TAB)
        }

        if (savedInstanceState == null) {
            // if there's no savedInstanceState, and we're not coming back from a Theme change,
            // then we must have been launched with an Intent, so... handle it!
            handleIntent(intent)
        }
    }

    fun animateTabsButton() {
        binding.pageToolbarButtonTabs.updateTabCount(true)
    }

    fun hideSoftKeyboard() {
        DeviceUtil.hideSoftKeyboard(this)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (!isDestroyed) {
            binding.pageToolbarButtonTabs.updateTabCount(false)
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (app.haveMainActivity()) {
                    onBackPressed()
                } else {
                    goToMainTab(NavTab.EXPLORE)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavMenuSwipeRequest(gravity: Int) {
        if (!isCabOpen && gravity == Gravity.END) {
            pageFragment.tocHandler?.show()
        }
    }

    private fun goToMainTab(navTab: NavTab) {
        startActivity(
            MainActivity.newIntent(this)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
                .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, navTab.code())
        )
        finish()
    }

    /** @return True if the contextual action bar is open.
     */
    private val isCabOpen get() = currentActionModes.isNotEmpty()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            var uri = intent.data
            if (isProdRelease && uri?.scheme != null && uri.scheme == "http") {
                // For external links, ensure that they're using https.
                uri = uri.buildUpon().scheme(WikiSite.DEFAULT_SCHEME).build()
            }
            uri?.let {
                val wiki = WikiSite(it)
                val title = wiki.titleForUri(it)
                val historyEntry = HistoryEntry(title, if (intent.hasExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID))
                    HistoryEntry.SOURCE_NOTIFICATION_SYSTEM else HistoryEntry.SOURCE_EXTERNAL_LINK)
                if (intent.hasExtra(Intent.EXTRA_REFERRER)) {
                    // Populate the referrer with the externally-referring URL, e.g. an external Browser URL.
                    // This can be a Uri or a String, so let's extract it safely as an Object.
                    historyEntry.referrer = intent.extras?.get(Intent.EXTRA_REFERRER)?.toString()
                }
                // Special cases:
                // If the link is to a page in the "donate." or "thankyou." domains (e.g. a "thank you" page
                // after having donated), then bounce it out to an external browser, since we don't have
                // the same cookie state as the browser does.
                val language = wiki.languageCode().toLowerCase(Locale.getDefault())
                val isDonationRelated = language == "donate" || language == "thankyou"
                if (isDonationRelated) {
                    visitInExternalBrowser(this, it)
                    finish()
                    return
                }
                loadPage(title, historyEntry, TabPosition.NEW_TAB_FOREGROUND)
            }
        } else if ((ACTION_LOAD_IN_NEW_TAB == intent.action || ACTION_LOAD_IN_CURRENT_TAB == intent.action ||
                    ACTION_LOAD_IN_CURRENT_TAB_SQUASH == intent.action) && intent.hasExtra(EXTRA_HISTORYENTRY)) {
            val title = intent.getParcelableExtra<PageTitle>(EXTRA_PAGETITLE)
            val historyEntry = intent.getParcelableExtra<HistoryEntry>(EXTRA_HISTORYENTRY)
            when (intent.action) {
                ACTION_LOAD_IN_NEW_TAB -> loadPage(title, historyEntry, TabPosition.NEW_TAB_FOREGROUND)
                ACTION_LOAD_IN_CURRENT_TAB -> loadPage(title, historyEntry, TabPosition.CURRENT_TAB)
                ACTION_LOAD_IN_CURRENT_TAB_SQUASH -> loadPage(title, historyEntry, TabPosition.CURRENT_TAB_SQUASH)
            }
            if (intent.hasExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER)) {
                showDescriptionEditRevertDialog(intent.getStringExtra(Constants.INTENT_EXTRA_REVERT_QNUMBER)!!)
            }
        } else if (ACTION_LOAD_FROM_EXISTING_TAB == intent.action && intent.hasExtra(EXTRA_HISTORYENTRY)) {
            val title = intent.getParcelableExtra<PageTitle>(EXTRA_PAGETITLE)
            val historyEntry = intent.getParcelableExtra<HistoryEntry>(EXTRA_HISTORYENTRY)
            loadPage(title, historyEntry, TabPosition.EXISTING_TAB)
        } else if (ACTION_RESUME_READING == intent.action || intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            loadFilePageFromBackStackIfNeeded()
        } else if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            val title = PageTitle(query, app.wikiSite)
            val historyEntry = HistoryEntry(title, HistoryEntry.SOURCE_SEARCH)
            loadPage(title, historyEntry, TabPosition.EXISTING_TAB)
        } else if (intent.hasExtra(Constants.INTENT_FEATURED_ARTICLE_FROM_WIDGET)) {
            IntentFunnel(app).logFeaturedArticleWidgetTap()
            val title = intent.getParcelableExtra<PageTitle>(EXTRA_PAGETITLE)
            title?.let {
                val historyEntry = HistoryEntry(it, HistoryEntry.SOURCE_WIDGET)
                loadPage(it, historyEntry, TabPosition.EXISTING_TAB)
            }
        } else if (ACTION_CREATE_NEW_TAB == intent.action) {
            loadMainPage(TabPosition.NEW_TAB_FOREGROUND)
        } else {
            loadMainPage(TabPosition.CURRENT_TAB)
        }
    }

    fun updateProgressBar(visible: Boolean) {
        binding.pageProgressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * Load a new page, and put it on top of the backstack, optionally allowing state loss of the
     * fragment manager. Useful for when this function is called from an AsyncTask result.
     * @param title Title of the page to load.
     * @param entry HistoryEntry associated with this page.
     * @param position Whether to open this page in the current tab, a new background tab, or new
     * foreground tab.
     */
    fun loadPage(title: PageTitle?, entry: HistoryEntry?, position: TabPosition) {
        if (isDestroyed || title == null || entry == null) {
            return
        }
        if (hasTransitionAnimation && !wasTransitionShown) {
            binding.pageFragment.visibility = View.GONE
            binding.wikiArticleCardView.prepareForTransition(title)
            wasTransitionShown = true
        }
        if (entry.source != HistoryEntry.SOURCE_INTERNAL_LINK || !Prefs.isLinkPreviewEnabled()) {
            LinkPreviewFunnel(app, entry.source).logNavigate()
        }
        app.putCrashReportProperty("api", title.wikiSite.authority())
        app.putCrashReportProperty("title", title.toString())
        if (loadNonArticlePageIfNeeded(title)) {
            return
        }
        binding.pageToolbarContainer.post {
            if (!pageFragment.isAdded) {
                return@post
            }

            // Close the link preview, if one is open.
            hideLinkPreview()
            onPageCloseActionMode()
            if (position == TabPosition.CURRENT_TAB) {
                pageFragment.loadPage(title, entry, true, false)
            } else if (position == TabPosition.CURRENT_TAB_SQUASH) {
                pageFragment.loadPage(title, entry, true, true)
            } else if (position == TabPosition.NEW_TAB_BACKGROUND) {
                pageFragment.openInNewBackgroundTab(title, entry)
            } else if (position == TabPosition.NEW_TAB_FOREGROUND) {
                pageFragment.openInNewForegroundTab(title, entry)
            } else {
                pageFragment.openFromExistingTab(title, entry)
            }
            app.sessionFunnel.pageViewed(entry)
        }
    }

    fun loadMainPage(position: TabPosition) {
        val title = PageTitle(getMainPageForLang(app.appOrSystemLanguageCode), app.wikiSite)
        val historyEntry = HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE)
        loadPage(title, historyEntry, position)
    }

    private fun loadFilePageFromBackStackIfNeeded() {
        if (pageFragment.currentTab.backStack.isNotEmpty()) {
            val item = pageFragment.currentTab.backStack[pageFragment.currentTab.backStackPosition]
            loadNonArticlePageIfNeeded(item.title)
        }
    }

    private fun loadNonArticlePageIfNeeded(title: PageTitle?): Boolean {
        if (title != null) {
            if (title.isFilePage) {
                startActivity(FilePageActivity.newIntent(this, title))
                finish()
                return true
            } else if (title.namespace() === Namespace.USER_TALK || title.namespace() === Namespace.TALK) {
                startActivity(TalkTopicsActivity.newIntent(this, title.pageTitleForTalkPage(), InvokeSource.PAGE_ACTIVITY))
                finish()
                return true
            }
        }
        return false
    }

    private fun hideLinkPreview() {
        bottomSheetPresenter.dismiss(supportFragmentManager)
    }

    fun showAddToListDialog(title: PageTitle, source: InvokeSource) {
        bottomSheetPresenter.showAddToListDialog(supportFragmentManager, title, source, listDialogDismissListener)
    }

    fun showMoveToListDialog(sourceReadingListId: Long, title: PageTitle, source: InvokeSource, showDefaultList: Boolean) {
        bottomSheetPresenter.showMoveToListDialog(supportFragmentManager, sourceReadingListId,
            title, source, showDefaultList, listDialogDismissListener)
    }

    override fun onPageLoadComplete() {
        removeTransitionAnimState()
        maybeShowWatchlistTooltip()
    }

    private fun removeTransitionAnimState() {
        if (binding.pageFragment.visibility != View.VISIBLE) {
            binding.pageFragment.visibility = View.VISIBLE
        }
        if (binding.wikiArticleCardView.visibility != View.GONE) {
            binding.wikiArticleCardView.postDelayed({ binding.wikiArticleCardView.visibility = View.GONE }, 250L)
        }
    }

    // Note: back button first handled in {@link #onOptionsItemSelected()};
    override fun onBackPressed() {
        if (isCabOpen) {
            onPageCloseActionMode()
            return
        }
        app.sessionFunnel.backPressed()
        if (pageFragment.onBackPressed()) {
            return
        }

        // If user enter PageActivity in portrait and leave in landscape,
        // we should hide the transition animation view to prevent bad animation.
        if (DimenUtil.isLandscape(this) || !hasTransitionAnimation) {
            binding.wikiArticleCardView.visibility = View.GONE
        } else {
            binding.wikiArticleCardView.visibility = View.VISIBLE
            binding.pageFragment.visibility = View.GONE
        }
        super.onBackPressed()
    }

    override fun onPageDismissBottomSheet() {
        bottomSheetPresenter.dismiss(supportFragmentManager)
    }

    override fun onPageInitWebView(webView: ObservableWebView) {
        toolbarHideHandler.setScrollView(webView)
    }

    override fun onPageLoadPage(title: PageTitle, entry: HistoryEntry) {
        loadPage(title, entry, TabPosition.CURRENT_TAB)
    }

    override fun onPageShowLinkPreview(entry: HistoryEntry) {
        bottomSheetPresenter.show(supportFragmentManager, LinkPreviewDialog.newInstance(entry, null))
    }

    override fun onPageLoadMainPageInForegroundTab() {
        loadMainPage(TabPosition.EXISTING_TAB)
    }

    override fun onPageUpdateProgressBar(visible: Boolean) {
        updateProgressBar(visible)
    }

    override fun onPageStartSupportActionMode(callback: ActionMode.Callback) {
        startActionMode(callback)
    }

    override fun onPageHideSoftKeyboard() {
        hideSoftKeyboard()
    }

    override fun onPageAddToReadingList(title: PageTitle, source: InvokeSource) {
        showAddToListDialog(title, source)
    }

    override fun onPageMoveToReadingList(sourceReadingListId: Long, title: PageTitle, source: InvokeSource, showDefaultList: Boolean) {
        showMoveToListDialog(sourceReadingListId, title, source, showDefaultList)
    }

    override fun onPageWatchlistExpirySelect(expiry: WatchlistExpiry) {
        watchlistFunnel.logAddExpiry()
        pageFragment.updateWatchlist(expiry, false)
    }

    override fun onPageLoadError(title: PageTitle) {
        supportActionBar?.title = title.displayText
        removeTransitionAnimState()
    }

    override fun onPageLoadErrorBackPressed() {
        finish()
    }

    override fun onPageSetToolbarElevationEnabled(enabled: Boolean) {
        binding.pageToolbarContainer.elevation = DimenUtil.dpToPx(if (enabled) DimenUtil.getDimension(R.dimen.toolbar_default_elevation) else 0F)
    }

    override fun onPageCloseActionMode() {
        val actionModesToFinish = HashSet(currentActionModes)
        for (mode in actionModesToFinish) {
            mode.finish()
        }
        currentActionModes.clear()
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        loadPage(title, entry, if (inNewTab) TabPosition.NEW_TAB_BACKGROUND else TabPosition.CURRENT_TAB)
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        copyLink(title.uri)
        showCopySuccessMessage()
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        showAddToListDialog(title, InvokeSource.LINK_PREVIEW_MENU)
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        shareText(this, title)
    }

    private fun copyLink(url: String) {
        setPlainText(this, null, url)
    }

    private fun showCopySuccessMessage() {
        showMessage(this, R.string.address_copied)
    }

    private fun showOverflowMenu(anchor: View) {
        val overflowView = PageActionOverflowView(this)
        overflowView.show(
            anchor,
            overflowCallback,
            pageFragment.currentTab,
            pageFragment.model.shouldLoadAsMobileWeb(),
            pageFragment.model.isWatched,
            pageFragment.model.hasWatchlistExpiry()
        )
    }

    private inner class OverflowCallback : PageActionOverflowView.Callback {
        override fun forwardClick() {
            pageFragment.goForward()
        }

        override fun watchlistClick(isWatched: Boolean) {
            if (isWatched) {
                watchlistFunnel.logRemoveArticle()
            } else {
                watchlistFunnel.logAddArticle()
            }
            pageFragment.updateWatchlist(WatchlistExpiry.NEVER, isWatched)
        }

        override fun shareClick() {
            pageFragment.sharePageLink()
        }

        override fun newTabClick() {
            startActivity(newIntentForNewTab(this@PageActivity))
        }

        override fun feedClick() {
            goToMainTab(NavTab.EXPLORE)
        }

        override fun talkClick() {
            startActivity(
                TalkTopicsActivity.newIntent(this@PageActivity, pageFragment.title.pageTitleForTalkPage(), InvokeSource.PAGE_ACTIVITY)
            )
        }

        override fun editHistoryClick() {
            visitInExternalBrowser(this@PageActivity, Uri.parse(pageFragment.title.getWebApiUrl("action=history")))
        }
    }

    override fun onResume() {
        super.onResume()
        app.resetWikiSite()
        Prefs.storeTemporaryWikitext(null)
    }

    public override fun onPause() {
        if (isCabOpen) {
            // Explicitly close any current ActionMode (see T147191)
            onPageCloseActionMode()
        }
        super.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(LANGUAGE_CODE_BUNDLE_KEY, app.appOrSystemLanguageCode)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_SETTINGS) {
            handleSettingsActivityResult(resultCode)
        } else if (newArticleLanguageSelected(requestCode, resultCode) || galleryPageSelected(requestCode, resultCode)) {
            data?.let {
                binding.pageToolbarContainer.post { handleIntent(it) }
            }
        } else if (galleryImageEdited(requestCode, resultCode)) {
            pageFragment.refreshPage()
        } else if (requestCode == Constants.ACTIVITY_REQUEST_BROWSE_TABS) {
            if (app.tabCount == 0 && resultCode != TabActivity.RESULT_NEW_TAB) {
                // They browsed the tabs and cleared all of them, without wanting to open a new tab.
                finish()
                return
            }
            if (resultCode == TabActivity.RESULT_NEW_TAB) {
                loadMainPage(TabPosition.NEW_TAB_FOREGROUND)
                animateTabsButton()
            } else if (resultCode == TabActivity.RESULT_LOAD_FROM_BACKSTACK) {
                pageFragment.reloadFromBackstack()
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL && resultCode == RESULT_OK) {
            Prefs.setDescriptionEditTutorialEnabled(false)
            data?.let {
                pageFragment.startDescriptionEditActivity(it.getStringExtra(DescriptionEditTutorialActivity.DESCRIPTION_SELECTED_TEXT))
            }
        } else if ((requestCode == Constants.ACTIVITY_REQUEST_IMAGE_CAPTION_EDIT || requestCode == Constants.ACTIVITY_REQUEST_IMAGE_TAGS_EDIT ||
                    requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT) && (resultCode == RESULT_OK ||
                    resultCode == DescriptionEditSuccessActivity.RESULT_OK_FROM_EDIT_SUCCESS)) {
            pageFragment.refreshPage()
            val editLanguage = pageFragment.leadImageEditLang.orEmpty().ifEmpty { app.language().appLanguageCode }
            val action = if (data != null && data.hasExtra(Constants.INTENT_EXTRA_ACTION))
                data.getSerializableExtra(Constants.INTENT_EXTRA_ACTION) as DescriptionEditActivity.Action?
            else if (requestCode == Constants.ACTIVITY_REQUEST_IMAGE_TAGS_EDIT) DescriptionEditActivity.Action.ADD_IMAGE_TAGS
            else null

            show(this, action, resultCode != DescriptionEditSuccessActivity.RESULT_OK_FROM_EDIT_SUCCESS,
                editLanguage, requestCode != Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT,
                OpenPageListener {
                    if (action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS) {
                        startActivity(FilePageActivity.newIntent(this, pageFragment.title))
                    } else if (action === DescriptionEditActivity.Action.ADD_CAPTION || action === DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                        startActivity(GalleryActivity.newIntent(this, pageFragment.title,
                            pageFragment.title.prefixedText, pageFragment.title.wikiSite, 0, GalleryFunnel.SOURCE_NON_LEAD_IMAGE))
                    }
                })
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    public override fun onDestroy() {
        disposables.clear()
        Prefs.setHasVisitedArticlePage(true)
        super.onDestroy()
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (!isCabOpen && mode.tag == null) {
            modifyMenu(mode)
            setCloseButtonInActionMode(pageFragment.requireContext(), mode)
            pageFragment.onActionModeShown(mode)
        }
        currentActionModes.add(mode)
    }

    private fun modifyMenu(mode: ActionMode) {
        val menu = mode.menu
        val menuItemsList = ArrayList<MenuItem>()
        for (i in 0 until menu.size()) {
            val title = menu.getItem(i).title.toString()
            if (!title.contains(getString(R.string.search_hint))
                && !(title.contains(getString(R.string.menu_text_select_define)) && pageFragment.shareHandler.shouldEnableWiktionaryDialog())
            ) {
                menuItemsList.add(menu.getItem(i))
            }
        }
        menu.clear()
        mode.menuInflater.inflate(R.menu.menu_text_select, menu)
        for (menuItem in menuItemsList) {
            menu.add(menuItem.groupId, menuItem.itemId, Menu.NONE, menuItem.title).setIntent(menuItem.intent).icon = menuItem.icon
        }
    }

    override fun onActionModeFinished(mode: ActionMode) {
        super.onActionModeFinished(mode)
        currentActionModes.remove(mode)
    }

    fun clearActionBarTitle() {
        supportActionBar?.title = ""
    }

    private fun handleSettingsActivityResult(resultCode: Int) {
        if (resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED) {
            loadNewLanguageMainPage()
        }
    }

    private fun loadNewLanguageMainPage() {
        val uiThread = Handler(Looper.getMainLooper())
        uiThread.postDelayed({
            loadMainPage(TabPosition.EXISTING_TAB)
            forceUpdateWidget(applicationContext)
        }, DateUtils.SECOND_IN_MILLIS)
    }

    private fun newArticleLanguageSelected(requestCode: Int, resultCode: Int): Boolean {
        return requestCode == Constants.ACTIVITY_REQUEST_LANGLINKS && resultCode == LangLinksActivity.ACTIVITY_RESULT_LANGLINK_SELECT
    }

    private fun galleryPageSelected(requestCode: Int, resultCode: Int): Boolean {
        return requestCode == Constants.ACTIVITY_REQUEST_GALLERY && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED
    }

    private fun galleryImageEdited(requestCode: Int, resultCode: Int): Boolean {
        return requestCode == Constants.ACTIVITY_REQUEST_GALLERY &&
                (resultCode == GalleryActivity.ACTIVITY_RESULT_IMAGE_CAPTION_ADDED || resultCode == GalleryActivity.ACTIVITY_REQUEST_ADD_IMAGE_TAGS)
    }

    private fun showDescriptionEditRevertDialog(qNumber: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_reverted_title)
            .setView(DescriptionEditRevertHelpView(this, qNumber))
            .setPositiveButton(R.string.reverted_edit_dialog_ok_button_text, null)
            .create()
            .show()
    }

    private fun maybeShowWatchlistTooltip() {
        if (!Prefs.isWatchlistPageOnboardingTooltipShown() && AccountUtil.isLoggedIn &&
            pageFragment.historyEntry != null && pageFragment.historyEntry.source != HistoryEntry.SOURCE_SUGGESTED_EDITS) {
            binding.pageToolbarButtonShowOverflowMenu.postDelayed({
                if (isDestroyed) {
                    return@postDelayed
                }
                watchlistFunnel.logShowTooltip()
                Prefs.setWatchlistPageOnboardingTooltipShown(true)
                showTooltip(this, binding.pageToolbarButtonShowOverflowMenu, R.layout.view_watchlist_page_tooltip,
                    -32, -8, aboveOrBelow = false, autoDismiss = false
                )
            }, 500)
        }
    }

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any?) {
            if (event is ChangeTextSizeEvent) {
                pageFragment.updateFontSize()
            } else if (event is ArticleSavedOrDeletedEvent) {
                if (!pageFragment.isAdded || pageFragment.title == null) {
                    return
                }
                for ((wiki, _, _, apiTitle) in event.pages) {
                    if (apiTitle == pageFragment.title.prefixedText && wiki.languageCode() == pageFragment.title.wikiSite.languageCode()) {
                        pageFragment.updateBookmarkAndMenuOptionsFromDao()
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_F || !event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_F3) {
            pageFragment.showFindInPage()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val LANGUAGE_CODE_BUNDLE_KEY = "language"
        const val ACTION_LOAD_IN_NEW_TAB = "org.wikipedia.load_in_new_tab"
        const val ACTION_LOAD_IN_CURRENT_TAB = "org.wikipedia.load_in_current_tab"
        const val ACTION_LOAD_IN_CURRENT_TAB_SQUASH = "org.wikipedia.load_in_current_tab_squash"
        const val ACTION_LOAD_FROM_EXISTING_TAB = "org.wikipedia.load_from_existing_tab"
        const val ACTION_CREATE_NEW_TAB = "org.wikipedia.create_new_tab"
        const val ACTION_RESUME_READING = "org.wikipedia.resume_reading"
        const val EXTRA_PAGETITLE = "org.wikipedia.pagetitle"
        const val EXTRA_HISTORYENTRY = "org.wikipedia.history.historyentry"

        fun newIntent(context: Context): Intent {
            return Intent(ACTION_RESUME_READING).setClass(context, PageActivity::class.java)
        }

        fun newIntentForNewTab(context: Context): Intent {
            return Intent(ACTION_CREATE_NEW_TAB)
                .setClass(context, PageActivity::class.java)
        }

        @JvmStatic
        fun newIntentForNewTab(
            context: Context,
            entry: HistoryEntry,
            title: PageTitle
        ): Intent {
            return Intent(ACTION_LOAD_IN_NEW_TAB)
                .setClass(context, PageActivity::class.java)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, title)
        }

        @JvmStatic
        @JvmOverloads
        fun newIntentForCurrentTab(context: Context, entry: HistoryEntry, title: PageTitle, squashBackstack: Boolean = true): Intent {
            return Intent(if (squashBackstack) ACTION_LOAD_IN_CURRENT_TAB_SQUASH else ACTION_LOAD_IN_CURRENT_TAB)
                .setClass(context, PageActivity::class.java)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, title)
        }

        fun newIntentForExistingTab(context: Context, entry: HistoryEntry, title: PageTitle): Intent {
            return Intent(ACTION_LOAD_FROM_EXISTING_TAB)
                .setClass(context, PageActivity::class.java)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, title)
        }
    }
}
