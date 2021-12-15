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
import android.text.format.DateUtils
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.*
import androidx.preference.PreferenceManager
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.*
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityPageBinding
import org.wikipedia.dataclient.WikiSite
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
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.search.SearchActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.SiteInfoClient
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.suggestededits.SuggestedEditsSnackbars
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.*
import org.wikipedia.views.*
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.widgets.WidgetProviderFeaturedPage
import java.util.*

class PageActivity : BaseActivity(), PageFragment.Callback, LinkPreviewDialog.Callback, FrameLayoutNavMenuTriggerer.Callback {

    enum class TabPosition {
        CURRENT_TAB, CURRENT_TAB_SQUASH, NEW_TAB_BACKGROUND, NEW_TAB_FOREGROUND, EXISTING_TAB
    }

    lateinit var binding: ActivityPageBinding
    private lateinit var toolbarHideHandler: ViewHideHandler
    private lateinit var pageFragment: PageFragment
    private var app = WikipediaApp.getInstance()
    private var hasTransitionAnimation = false
    private var wasTransitionShown = false
    private val currentActionModes = mutableSetOf<ActionMode>()
    private val disposables = CompositeDisposable()
    private val overflowCallback = OverflowCallback()
    private val watchlistFunnel = WatchlistFunnel()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val listDialogDismissListener = DialogInterface.OnDismissListener { pageFragment.updateBookmarkAndMenuOptionsFromDao() }
    private val isCabOpen get() = currentActionModes.isNotEmpty()
    private var exclusiveTooltipRunnable: Runnable? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        binding = ActivityPageBinding.inflate(layoutInflater)

        try {
            setContentView(binding.root)
        } catch (e: Exception) {
            if (!e.message.isNullOrEmpty() && e.message!!.lowercase(Locale.getDefault()).contains(EXCEPTION_MESSAGE_WEBVIEW) ||
                !ThrowableUtil.getInnermostThrowable(e).message.isNullOrEmpty() &&
                ThrowableUtil.getInnermostThrowable(e).message!!.lowercase(Locale.getDefault()).contains(EXCEPTION_MESSAGE_WEBVIEW)) {
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
            startActivity(SearchActivity.newIntent(this@PageActivity, InvokeSource.TOOLBAR, null))
        }
        binding.pageToolbarButtonTabs.setColor(ResourceUtil.getThemedColor(this, R.attr.toolbar_icon_color))
        binding.pageToolbarButtonTabs.updateTabCount(false)
        binding.pageToolbarButtonTabs.setOnClickListener {
            TabActivity.captureFirstTabBitmap(pageFragment.containerView)
            startActivityForResult(TabActivity.newIntentFromPageActivity(this), Constants.ACTIVITY_REQUEST_BROWSE_TABS)
        }
        toolbarHideHandler = ViewHideHandler(binding.pageToolbarContainer, null, Gravity.TOP)
        FeedbackUtil.setButtonLongPressToast(binding.pageToolbarButtonNotifications, binding.pageToolbarButtonTabs, binding.pageToolbarButtonShowOverflowMenu)
        binding.pageToolbarButtonShowOverflowMenu.setOnClickListener {
            showOverflowMenu(it)
        }

        binding.pageToolbarButtonNotifications.setColor(ResourceUtil.getThemedColor(this, R.attr.toolbar_icon_color))
        binding.pageToolbarButtonNotifications.isVisible = AccountUtil.isLoggedIn
        binding.pageToolbarButtonNotifications.setOnClickListener {
            if (AccountUtil.isLoggedIn) {
                startActivity(NotificationActivity.newIntent(this@PageActivity))
            } else if (AnonymousNotificationHelper.isWithinAnonNotificationTime() && !Prefs.lastAnonNotificationLang.isNullOrEmpty()) {
                val wikiSite = WikiSite.forLanguageCode(Prefs.lastAnonNotificationLang!!)
                startActivity(TalkTopicsActivity.newIntent(this@PageActivity,
                PageTitle(UserTalkAliasData.valueFor(wikiSite.languageCode) + ":" + Prefs.lastAnonUserWithMessages, wikiSite), InvokeSource.PAGE_ACTIVITY))
            }
        }

        // Navigation setup
        binding.navigationDrawer.setScrimColor(Color.TRANSPARENT)
        binding.containerWithNavTrigger.callback = this
        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationDrawer) { _, insets ->
            val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.pageToolbarContainer.updatePadding(top = systemWindowInsets.top)
            pageFragment.updateInsets(systemWindowInsets)
            insets
        }

        // WikiArticleCard setup
        hasTransitionAnimation = intent.getBooleanExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, false)
        binding.wikiArticleCardView.visibility = if (hasTransitionAnimation) View.VISIBLE else View.GONE

        val languageChanged = savedInstanceState?.let {
            app.appOrSystemLanguageCode != savedInstanceState.getString(LANGUAGE_CODE_BUNDLE_KEY).orEmpty()
        } ?: false

        if (languageChanged) {
            app.resetWikiSite()
            loadMainPage(TabPosition.EXISTING_TAB)
        }

        if (AccountUtil.isLoggedIn) {
            Prefs.loggedInPageActivityVisitCount++
        }

        if (savedInstanceState == null) {
            // if there's no savedInstanceState, and we're not coming back from a Theme change,
            // then we must have been launched with an Intent, so... handle it!
            handleIntent(intent)
        }
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
                    goToMainTab()
                }
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        app.resetWikiSite()
        updateNotificationsButton(false)
        Prefs.temporaryWikitext = null
    }

    override fun onPause() {
        if (isCabOpen) {
            onPageCloseActionMode()
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(LANGUAGE_CODE_BUNDLE_KEY, app.appOrSystemLanguageCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_SETTINGS) {
            handleSettingsActivityResult(resultCode)
        } else if (newArticleLanguageSelected(requestCode, resultCode) || galleryPageSelected(requestCode, resultCode)) {
            data?.let {
                binding.pageToolbarContainer.post { handleIntent(it) }
            }
        } else if (galleryImageEdited(requestCode, resultCode)) {
            pageFragment.reloadFromBackstack()
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
            Prefs.isDescriptionEditTutorialEnabled = false
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

            SuggestedEditsSnackbars.show(this, action, resultCode != DescriptionEditSuccessActivity.RESULT_OK_FROM_EDIT_SUCCESS,
                editLanguage, requestCode != Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT) {
                pageFragment.title?.let {
                    if (action === DescriptionEditActivity.Action.ADD_IMAGE_TAGS) {
                        startActivity(FilePageActivity.newIntent(this, it))
                    } else if (action === DescriptionEditActivity.Action.ADD_CAPTION || action === DescriptionEditActivity.Action.TRANSLATE_CAPTION) {
                        startActivity(GalleryActivity.newIntent(this, it, it.prefixedText, it.wikiSite, 0, GalleryFunnel.SOURCE_NON_LEAD_IMAGE))
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (!isCabOpen && mode.tag == null) {
            modifyMenu(mode)
            ViewUtil.setCloseButtonInActionMode(pageFragment.requireContext(), mode)
            pageFragment.onActionModeShown(mode)
        }
        currentActionModes.add(mode)
    }

    override fun onActionModeFinished(mode: ActionMode) {
        super.onActionModeFinished(mode)
        currentActionModes.remove(mode)
    }

    override fun onDestroy() {
        disposables.clear()
        Prefs.hasVisitedArticlePage = true
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_F || !event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_F3) {
            pageFragment.showFindInPage()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNavMenuSwipeRequest(gravity: Int) {
        if (!isCabOpen && gravity == Gravity.END) {
            pageFragment.tocHandler.show()
        }
    }

    override fun onPageLoadComplete() {
        removeTransitionAnimState()
        maybeShowWatchlistTooltip()
    }

    override fun onPageDismissBottomSheet() {
        bottomSheetPresenter.dismiss(supportFragmentManager)
    }

    override fun onPageInitWebView(v: ObservableWebView) {
        toolbarHideHandler.setScrollView(v)
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
        DeviceUtil.hideSoftKeyboard(this)
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
        ShareUtil.shareText(this, title)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            var uri = intent.data
            if (ReleaseUtil.isProdRelease && uri?.scheme != null && uri.scheme == "http") {
                // For external links, ensure that they're using https.
                uri = uri.buildUpon().scheme(WikiSite.DEFAULT_SCHEME).build()
            }
            uri?.let {
                val wiki = WikiSite(it)
                val title = wiki.titleForUri(it)
                val historyEntry = HistoryEntry(title, if (intent.hasExtra(Constants.INTENT_EXTRA_NOTIFICATION_ID))
                    HistoryEntry.SOURCE_NOTIFICATION_SYSTEM else HistoryEntry.SOURCE_EXTERNAL_LINK)
                // Populate the referrer with the externally-referring URL, e.g. an external Browser URL, if present.
                ActivityCompat.getReferrer(this)?.let { uri ->
                    historyEntry.referrer = uri.toString()
                }
                // Special cases:
                // If the link is to a page in the "donate." or "thankyou." domains (e.g. a "thank you" page
                // after having donated), then bounce it out to an external browser, since we don't have
                // the same cookie state as the browser does.
                val language = wiki.languageCode.lowercase(Locale.getDefault())
                val isDonationRelated = language == "donate" || language == "thankyou"
                if (isDonationRelated || title.namespace() == Namespace.SPECIAL) {
                    UriUtil.visitInExternalBrowser(this, it)
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

    /**
     * Load a new page, and put it on top of the backstack, optionally allowing state loss of the
     * fragment manager. Useful for when this function is called from an AsyncTask result.
     * @param title Title of the page to load.
     * @param entry HistoryEntry associated with this page.
     * @param position Whether to open this page in the current tab, a new background tab, or new
     * foreground tab.
     */
    private fun loadPage(title: PageTitle?, entry: HistoryEntry?, position: TabPosition) {
        if (isDestroyed || title == null || entry == null) {
            return
        }
        if (hasTransitionAnimation && !wasTransitionShown) {
            binding.pageFragment.visibility = View.GONE
            binding.wikiArticleCardView.prepareForTransition(title)
            wasTransitionShown = true
        }
        if (entry.source != HistoryEntry.SOURCE_INTERNAL_LINK || !Prefs.isLinkPreviewEnabled) {
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
            when (position) {
                TabPosition.CURRENT_TAB -> pageFragment.loadPage(title, entry, pushBackStack = true, squashBackstack = false)
                TabPosition.CURRENT_TAB_SQUASH -> pageFragment.loadPage(title, entry, pushBackStack = true, squashBackstack = true)
                TabPosition.NEW_TAB_BACKGROUND -> pageFragment.openInNewBackgroundTab(title, entry)
                TabPosition.NEW_TAB_FOREGROUND -> pageFragment.openInNewForegroundTab(title, entry)
                else -> pageFragment.openFromExistingTab(title, entry)
            }
            app.sessionFunnel.pageViewed(entry)
        }
    }

    private fun goToMainTab() {
        startActivity(MainActivity.newIntent(this)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
            .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()))
        finish()
    }

    private fun loadMainPage(position: TabPosition) {
        val title = PageTitle(SiteInfoClient.getMainPageForLang(app.appOrSystemLanguageCode), app.wikiSite)
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
                startActivity(TalkTopicsActivity.newIntent(this, title, InvokeSource.PAGE_ACTIVITY))
                finish()
                return true
            }
        }
        return false
    }

    private fun updateProgressBar(visible: Boolean) {
        binding.pageProgressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun hideLinkPreview() {
        bottomSheetPresenter.dismiss(supportFragmentManager)
    }

    private fun showAddToListDialog(title: PageTitle, source: InvokeSource) {
        bottomSheetPresenter.showAddToListDialog(supportFragmentManager, title, source, listDialogDismissListener)
    }

    private fun showMoveToListDialog(sourceReadingListId: Long, title: PageTitle, source: InvokeSource, showDefaultList: Boolean) {
        bottomSheetPresenter.showMoveToListDialog(supportFragmentManager, sourceReadingListId,
            title, source, showDefaultList, listDialogDismissListener)
    }

    private fun removeTransitionAnimState() {
        if (binding.pageFragment.visibility != View.VISIBLE) {
            binding.pageFragment.visibility = View.VISIBLE
        }
        if (binding.wikiArticleCardView.visibility != View.GONE) {
            binding.wikiArticleCardView.postDelayed({ binding.wikiArticleCardView.visibility = View.GONE }, 250L)
        }
    }

    private fun copyLink(url: String) {
        ClipboardUtil.setPlainText(this, null, url)
    }

    private fun showCopySuccessMessage() {
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    private fun showOverflowMenu(anchor: View) {
        PageActionOverflowView(this).show(anchor, overflowCallback, pageFragment.currentTab,
            pageFragment.model.shouldLoadAsMobileWeb, pageFragment.model.isWatched, pageFragment.model.hasWatchlistExpiry
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
            goToMainTab()
        }

        override fun talkClick() {
            pageFragment.title?.let {
                startActivity(TalkTopicsActivity.newIntent(this@PageActivity, it, InvokeSource.PAGE_ACTIVITY))
            }
        }

        override fun editHistoryClick() {
            pageFragment.title?.run {
                UriUtil.visitInExternalBrowser(this@PageActivity, Uri.parse(getWebApiUrl("action=history")))
            }
        }
    }

    private fun modifyMenu(mode: ActionMode) {
        val menu = mode.menu
        val menuItemsList = menu.children.filter {
            val title = it.title.toString()
            !title.contains(getString(R.string.search_hint)) &&
                    !(title.contains(getString(R.string.menu_text_select_define)) &&
                            pageFragment.shareHandler.shouldEnableWiktionaryDialog())
        }.toList()
        menu.clear()
        mode.menuInflater.inflate(R.menu.menu_text_select, menu)
        menuItemsList.forEach {
            menu.add(it.groupId, it.itemId, Menu.NONE, it.title).setIntent(it.intent).icon = it.icon
        }
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
            WidgetProviderFeaturedPage.forceUpdateWidget(applicationContext)
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
        pageFragment.historyEntry?.let {
            if (!Prefs.isWatchlistPageOnboardingTooltipShown && AccountUtil.isLoggedIn &&
                    it.source != HistoryEntry.SOURCE_SUGGESTED_EDITS &&
                    Prefs.loggedInPageActivityVisitCount >= 3) {
                enqueueTooltip {
                    watchlistFunnel.logShowTooltip()
                    Prefs.isWatchlistPageOnboardingTooltipShown = true
                    FeedbackUtil.showTooltip(this, binding.pageToolbarButtonShowOverflowMenu,
                        R.layout.view_watchlist_page_tooltip, -32, -8, aboveOrBelow = false, autoDismiss = false)
                }
            }
        }
    }

    // TODO: remove on March 2022.
    private fun maybeShowNotificationTooltip() {
        if (!Prefs.isPageNotificationTooltipShown && AccountUtil.isLoggedIn &&
                Prefs.loggedInPageActivityVisitCount >= 1) {
            enqueueTooltip {
                FeedbackUtil.showTooltip(this, binding.pageToolbarButtonNotifications, getString(R.string.page_notification_tooltip),
                    aboveOrBelow = false, autoDismiss = false, -32, -8).setOnBalloonDismissListener {
                    Prefs.isPageNotificationTooltipShown = true
                }
            }
        }
    }

    private fun enqueueTooltip(runnable: Runnable) {
        if (exclusiveTooltipRunnable != null) {
            return
        }
        exclusiveTooltipRunnable = runnable
        binding.pageToolbar.postDelayed({
            exclusiveTooltipRunnable = null
            if (isDestroyed) {
                return@postDelayed
            }
            runnable.run()
        }, 500)
    }

    fun animateTabsButton() {
        toolbarHideHandler.ensureDisplayed()
        binding.pageToolbarButtonTabs.updateTabCount(true)
    }

    private fun updateNotificationsButton(animate: Boolean) {
        if (AccountUtil.isLoggedIn) {
            binding.pageToolbarButtonNotifications.isVisible = true
            if (Prefs.notificationUnreadCount > 0) {
                binding.pageToolbarButtonNotifications.setUnreadCount(Prefs.notificationUnreadCount)
                if (animate) {
                    toolbarHideHandler.ensureDisplayed()
                    binding.pageToolbarButtonNotifications.runAnimation()
                }
            } else {
                binding.pageToolbarButtonNotifications.setUnreadCount(0)
            }
        } else if (!AccountUtil.isLoggedIn && AnonymousNotificationHelper.isWithinAnonNotificationTime()) {
            binding.pageToolbarButtonNotifications.isVisible = true
            if (Prefs.hasAnonymousNotification) {
                binding.pageToolbarButtonNotifications.setUnreadCount(1)
                if (animate) {
                    toolbarHideHandler.ensureDisplayed()
                    binding.pageToolbarButtonNotifications.runAnimation()
                }
            } else {
                binding.pageToolbarButtonNotifications.setUnreadCount(0)
            }
        } else {
            binding.pageToolbarButtonNotifications.isVisible = false
        }
        maybeShowNotificationTooltip()
    }

    fun clearActionBarTitle() {
        supportActionBar?.title = ""
    }

    fun getToolbarMargin(): Int {
        return binding.pageToolbarContainer.height
    }

    override fun onUnreadNotification() {
        updateNotificationsButton(true)
    }

    fun onAnonNotification() {
        updateNotificationsButton(true)
    }

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any?) {
            when (event) {
                is ChangeTextSizeEvent -> {
                    pageFragment.updateFontSize()
                }
                is ArticleSavedOrDeletedEvent -> {
                    if (!pageFragment.isAdded) {
                        return
                    }
                    pageFragment.title?.run {
                        if (event.pages.any { it.apiTitle == prefixedText && it.wiki.languageCode == wikiSite.languageCode }) {
                            pageFragment.updateBookmarkAndMenuOptionsFromDao()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val LANGUAGE_CODE_BUNDLE_KEY = "language"
        private const val EXCEPTION_MESSAGE_WEBVIEW = "webview"
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
            return Intent(ACTION_CREATE_NEW_TAB).setClass(context, PageActivity::class.java)
        }

        @JvmStatic
        fun newIntentForNewTab(context: Context, entry: HistoryEntry, title: PageTitle): Intent {
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
