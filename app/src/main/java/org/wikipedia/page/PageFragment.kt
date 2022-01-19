package org.wikipedia.page

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.wikipedia.*
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.*
import org.wikipedia.auth.AccountUtil
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentPageBinding
import org.wikipedia.databinding.GroupFindReferencesInPageBinding
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.dataclient.watch.Watch
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.descriptions.DescriptionEditTutorialActivity
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.edit.EditHandler
import org.wikipedia.feed.announcement.Announcement
import org.wikipedia.feed.announcement.AnnouncementClient
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.language.LangLinksActivity
import org.wikipedia.login.LoginActivity
import org.wikipedia.media.AvPlayer
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.page.PageCacher.loadIntoCache
import org.wikipedia.page.action.PageActionTab
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.references.PageReferences
import org.wikipedia.page.references.ReferenceDialog
import org.wikipedia.page.shareafact.ShareHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.theme.ThemeChooserDialog
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView
import org.wikipedia.views.ViewUtil
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog
import org.wikipedia.wiktionary.WiktionaryDialog
import java.util.*

class PageFragment : Fragment(), BackPressedHandler, CommunicationBridge.CommunicationBridgeListener, ThemeChooserDialog.Callback,
    ReferenceDialog.Callback, WiktionaryDialog.Callback, WatchlistExpiryDialog.Callback {

    interface Callback {
        fun onPageDismissBottomSheet()
        fun onPageLoadComplete()
        fun onPageLoadPage(title: PageTitle, entry: HistoryEntry)
        fun onPageInitWebView(v: ObservableWebView)
        fun onPageShowLinkPreview(entry: HistoryEntry)
        fun onPageLoadMainPageInForegroundTab()
        fun onPageUpdateProgressBar(visible: Boolean)
        fun onPageStartSupportActionMode(callback: ActionMode.Callback)
        fun onPageHideSoftKeyboard()
        fun onPageAddToReadingList(title: PageTitle, source: InvokeSource)
        fun onPageMoveToReadingList(sourceReadingListId: Long, title: PageTitle, source: InvokeSource, showDefaultList: Boolean)
        fun onPageWatchlistExpirySelect(expiry: WatchlistExpiry)
        fun onPageLoadError(title: PageTitle)
        fun onPageLoadErrorBackPressed()
        fun onPageSetToolbarElevationEnabled(enabled: Boolean)
        fun onPageCloseActionMode()
    }

    private var _binding: FragmentPageBinding? = null
    private val binding get() = _binding!!

    private val activeTimer = ActiveTimer()
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val disposables = CompositeDisposable()
    private val scrollTriggerListener = WebViewScrollTriggerListener()
    private val tabFunnel = TabFunnel()
    private val watchlistFunnel = WatchlistFunnel()
    private val pageRefreshListener = OnRefreshListener { refreshPage() }
    private val pageActionTabsCallback = PageActionTabCallback()

    private lateinit var bridge: CommunicationBridge
    private lateinit var leadImagesHandler: LeadImagesHandler
    private lateinit var pageFragmentLoadState: PageFragmentLoadState
    private lateinit var bottomBarHideHandler: ViewHideHandler
    private var pageScrollFunnel: PageScrollFunnel? = null
    private var pageRefreshed = false
    private var errorState = false
    private var watchlistExpiryChanged = false
    private var scrolledUpForThemeChange = false
    private var references: PageReferences? = null
    private var avPlayer: AvPlayer? = null
    private var avCallback: AvCallback? = null
    private var sections: MutableList<Section>? = null
    private var app = WikipediaApp.getInstance()

    override lateinit var linkHandler: LinkHandler
    override lateinit var webView: ObservableWebView
    override var model = PageViewModel()
    override val toolbarMargin get() = (requireActivity() as PageActivity).getToolbarMargin()
    override val isPreview get() = false
    override val referencesGroup get() = references?.referencesGroup
    override val selectedReferenceIndex get() = references?.selectedIndex ?: 0

    lateinit var tocHandler: ToCHandler
    lateinit var shareHandler: ShareHandler
    lateinit var editHandler: EditHandler
    var revision = 0L

    private val shouldCreateNewTab get() = currentTab.backStack.isNotEmpty()
    private val backgroundTabPosition get() = 0.coerceAtLeast(foregroundTabPosition - 1)
    private val foregroundTabPosition get() = app.tabList.size
    private val tabLayoutOffsetParams get() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, binding.pageActionsTabLayout.height)
    val currentTab get() = app.tabList[app.tabList.size - 1]!!
    val title get() = model.title
    val page get() = model.page
    val historyEntry get() = model.curEntry
    val containerView get() = binding.pageContentsContainer
    val headerView get() = binding.pageHeaderView
    val isLoading get() = bridge.isLoading
    val leadImageEditLang get() = leadImagesHandler.callToActionEditLang

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPageBinding.inflate(inflater, container, false)
        webView = binding.pageWebView
        initWebViewListeners()
        binding.pageRefreshContainer.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent))
        binding.pageRefreshContainer.scrollableChild = webView
        binding.pageRefreshContainer.setOnRefreshListener(pageRefreshListener)
        val swipeOffset = DimenUtil.getContentTopOffsetPx(requireActivity()) + REFRESH_SPINNER_ADDITIONAL_OFFSET
        binding.pageRefreshContainer.setProgressViewOffset(false, -swipeOffset, swipeOffset)
        binding.pageActionsTabLayout.setPageActionTabsCallback(pageActionTabsCallback)
        savedInstanceState?.let {
            scrolledUpForThemeChange = it.getBoolean(ARG_THEME_CHANGE_SCROLLED, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        callback()?.onPageInitWebView(webView)
        updateFontSize()

        // Explicitly set background color of the WebView (independently of CSS, because
        // the background may be shown momentarily while the WebView loads content,
        // creating a seizure-inducing effect, or at the very least, a migraine with aura).
        val activity = requireActivity()
        webView.setBackgroundColor(ResourceUtil.getThemedColor(activity, R.attr.paper_color))
        bridge = CommunicationBridge(this)
        setupMessageHandlers()

        binding.pageError.retryClickListener = View.OnClickListener { refreshPage() }
        binding.pageError.backClickListener = View.OnClickListener {
            if (!onBackPressed()) {
                callback()?.onPageLoadErrorBackPressed()
            }
        }

        bottomBarHideHandler = ViewHideHandler(binding.pageActionsTabLayout, null, Gravity.BOTTOM, updateElevation = false)
        bottomBarHideHandler.setScrollView(webView)
        bottomBarHideHandler.enabled = Prefs.readingFocusModeEnabled

        editHandler = EditHandler(this, bridge)
        tocHandler = ToCHandler(this, ActivityCompat.requireViewById(activity, R.id.navigation_drawer),
            ActivityCompat.requireViewById(activity, R.id.page_scroller_button), bridge)
        leadImagesHandler = LeadImagesHandler(this, webView, binding.pageHeaderView)
        shareHandler = ShareHandler(this, bridge)
        pageFragmentLoadState = PageFragmentLoadState(model, this, webView, bridge, leadImagesHandler, currentTab)

        if (callback() != null) {
            LongPressHandler(webView, HistoryEntry.SOURCE_INTERNAL_LINK, PageContainerLongPressHandler(this))
        }

        if (shouldLoadFromBackstack(activity) || savedInstanceState != null) {
            reloadFromBackstack()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_EDIT_SECTION && resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            FeedbackUtil.showMessage(requireActivity(), R.string.edit_saved_successfully)
            // and reload the page...
            model.title?.let { title ->
                model.curEntry?.let { entry ->
                    loadPage(title, entry, pushBackStack = false, squashBackstack = false, isRefresh = true)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(ARG_THEME_CHANGE_SCROLLED, scrolledUpForThemeChange)
    }

    override fun onDestroyView() {
        avPlayer?.let {
            it.deinit()
            avPlayer = null
        }
        // uninitialize the bridge, so that no further JS events can have any effect.
        bridge.cleanup()
        tocHandler.log()
        shareHandler.dispose()
        leadImagesHandler.dispose()
        disposables.clear()
        webView.clearAllListeners()
        (webView.parent as ViewGroup).removeView(webView)
        Prefs.isSuggestedEditsHighestPriorityEnabled = false
        _binding = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        activeTimer.pause()
        addTimeSpentReading(activeTimer.elapsedSec)
        pageFragmentLoadState.updateCurrentBackStackItem()
        app.commitTabState()
        closePageScrollFunnel()
        val time = if (app.tabList.size >= 1 && !pageFragmentLoadState.backStackEmpty()) System.currentTimeMillis() else 0
        Prefs.pageLastShown = time
    }

    override fun onResume() {
        super.onResume()
        initPageScrollFunnel()
        activeTimer.resume()
        val params = CoordinatorLayout.LayoutParams(1, 1)
        binding.pageImageTransitionHolder.layoutParams = params
        binding.pageImageTransitionHolder.visibility = View.GONE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // if the screen orientation changes, then re-layout the lead image container,
        // but only if we've finished fetching the page.
        if (!bridge.isLoading && !errorState) {
            pageFragmentLoadState.onConfigurationChanged()
        }
    }

    override fun onBackPressed(): Boolean {
        if (tocHandler.isVisible) {
            tocHandler.hide()
            return true
        }
        if (pageFragmentLoadState.goBack()) {
            return true
        }
        // if the current tab can no longer go back, then close the tab before exiting
        if (app.tabList.isNotEmpty()) {
            app.tabList.removeAt(app.tabList.size - 1)
            app.commitTabState()
        }
        return false
    }

    override fun onToggleDimImages() {
        ActivityCompat.recreate(requireActivity())
    }

    override fun onToggleReadingFocusMode() {
        webView.scrollEventsEnabled = false
        bottomBarHideHandler.enabled = Prefs.readingFocusModeEnabled
        leadImagesHandler.refreshCallToActionVisibility()
        page?.let {
            bridge.execute(JavaScriptActionHandler.setUpEditButtons(!Prefs.readingFocusModeEnabled, !it.pageProperties.canEdit))
        }
        binding.root.postDelayed({
            if (isAdded) {
                webView.scrollEventsEnabled = true
            }
        }, 250)
    }

    override fun onCancelThemeChooser() {
        if (scrolledUpForThemeChange) {
            val animDuration = 250L
            ObjectAnimator.ofInt(webView, "scrollY", webView.scrollY, 0)
                .setDuration(animDuration)
                .start()
        }
    }

    override fun wiktionaryShowDialogForTerm(term: String) {
        shareHandler.showWiktionaryDefinition(term)
    }

    override fun onExpirySelect(expiry: WatchlistExpiry) {
        callback()?.onPageWatchlistExpirySelect(expiry)
        dismissBottomSheet()
    }

    private fun shouldLoadFromBackstack(activity: Activity): Boolean {
        return (activity.intent != null && (PageActivity.ACTION_RESUME_READING == activity.intent.action ||
                activity.intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)))
    }

    private fun initWebViewListeners() {
        webView.addOnUpOrCancelMotionEventListener {
            // update our session, since it's possible for the user to remain on the page for
            // a long time, and we wouldn't want the session to time out.
            app.sessionFunnel.touchSession()
        }
        webView.addOnScrollChangeListener { oldScrollY, scrollY, isHumanScroll ->
            pageScrollFunnel?.onPageScrolled(oldScrollY, scrollY, isHumanScroll)
        }
        webView.addOnContentHeightChangedListener(scrollTriggerListener)
        webView.webViewClient = object : OkHttpWebViewClient() {

            override val model get() = this@PageFragment.model

            override val linkHandler get() = this@PageFragment.linkHandler

            override fun onPageFinished(view: WebView, url: String) {
                bridge.evaluateImmediate("(function() { return (typeof pcs !== 'undefined'); })();") { pcsExists ->
                    if (!isAdded) {
                        return@evaluateImmediate
                    }
                    // TODO: This is a bit of a hack: If PCS does not exist in the current page, then
                    // it's implied that this page was loaded via Mobile Web (e.g. the Main Page) and
                    // doesn't support PCS, meaning that we will never receive the `setup` event that
                    // tells us the page is finished loading. In such a case, we must infer that the
                    // page has now loaded and trigger the remaining logic ourselves.
                    if ("true" != pcsExists) {
                        onPageSetupEvent()
                        bridge.onMetadataReady()
                        bridge.onPcsReady()
                        bridge.execute(JavaScriptActionHandler.mobileWebChromeShim())
                    }
                }
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                onPageLoadError(RuntimeException(description))
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (!request.url.toString().contains(RestService.PAGE_HTML_ENDPOINT)) {
                    // If the request is anything except the main mobile-html content request, then
                    // don't worry about any errors and let the WebView deal with it.
                    return
                }
                onPageLoadError(HttpStatusException(errorResponse.statusCode, request.url.toString(), UriUtil.decodeURL(errorResponse.reasonPhrase)))
            }
        }
    }

    private fun onPageSetupEvent() {
        if (!isAdded) {
            return
        }
        updateProgressBar(false)
        webView.visibility = View.VISIBLE
        app.sessionFunnel.leadSectionFetchEnd()
        bridge.evaluate(JavaScriptActionHandler.getRevision()) { value ->
            if (!isAdded || value == null || value == "null") {
                return@evaluate
            }
            try {
                revision = value.replace("\"", "").toLong()
            } catch (e: Exception) {
                L.e(e)
            }
        }
        bridge.evaluate(JavaScriptActionHandler.getSections()) { value ->
            if (!isAdded) {
                return@evaluate
            }
            model.page?.let { page ->
                sections = JsonUtil.decodeFromString(value)
                sections?.let { sections ->
                    sections.add(0, Section(0, 0, model.title?.displayText.orEmpty(), model.title?.displayText.orEmpty(), ""))
                    page.sections = sections
                }
            }

            model.title?.let {
                tocHandler.setupToC(model.page, it.wikiSite)
                tocHandler.setEnabled(true)
            }
        }
        bridge.evaluate(JavaScriptActionHandler.getProtection()) { value ->
            if (!isAdded) {
                return@evaluate
            }
            model.page?.let { page ->
                page.pageProperties.protection = JsonUtil.decodeFromString(value)
                bridge.execute(JavaScriptActionHandler.setUpEditButtons(!Prefs.readingFocusModeEnabled, !page.pageProperties.canEdit))
            }
        }
    }

    private fun handleInternalLink(title: PageTitle) {
        if (!isResumed) {
            return
        }
        if (title.namespace() === Namespace.USER_TALK || title.namespace() === Namespace.TALK) {
            startTalkTopicActivity(title)
            return
        }
        dismissBottomSheet()
        val historyEntry = HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK)
        model.title?.run {
            historyEntry.referrer = uri
        }
        if (title.namespace() !== Namespace.MAIN || !Prefs.isLinkPreviewEnabled) {
            loadPage(title, historyEntry)
        } else {
            callback()?.onPageShowLinkPreview(historyEntry)
        }
    }

    private fun setCurrentTabAndReset(position: Int) {
        // move the selected tab to the bottom of the list, and navigate to it!
        // (but only if it's a different tab than the one currently in view!
        if (position < app.tabList.size - 1) {
            val tab = app.tabList.removeAt(position)
            app.tabList.add(tab)
            pageFragmentLoadState.setTab(tab)
        }
        if (app.tabCount > 0) {
            app.tabList[app.tabList.size - 1].squashBackstack()
            pageFragmentLoadState.loadFromBackStack()
        }
    }

    private fun openInNewTab(title: PageTitle, entry: HistoryEntry, position: Int) {
        val selectedTabPosition = app.tabList.firstOrNull { it.backStackPositionTitle != null &&
                it.backStackPositionTitle == title }?.let { app.tabList.indexOf(it) } ?: -1

        if (selectedTabPosition >= 0) {
            return
        }
        tabFunnel.logOpenInNew(app.tabList.size)
        if (shouldCreateNewTab) {
            // create a new tab
            val tab = Tab()
            val isForeground = position == foregroundTabPosition
            // if the requested position is at the top, then make its backstack current
            if (isForeground) {
                pageFragmentLoadState.setTab(tab)
            }
            // put this tab in the requested position
            app.tabList.add(position, tab)
            trimTabCount()
            // add the requested page to its backstack
            tab.backStack.add(PageBackStackItem(title, entry))
            if (!isForeground) {
                loadIntoCache(title)
            }
            requireActivity().invalidateOptionsMenu()
        } else {
            pageFragmentLoadState.setTab(currentTab)
            currentTab.backStack.add(PageBackStackItem(title, entry))
        }
    }

    private fun setBottomBarButtonEnabled(button: PageActionTab, enabled: Boolean) {
        val view = binding.pageActionsTabLayout.getChildAt(button.code())
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.5f
    }

    private fun closePageScrollFunnel() {
        if (webView.contentHeight > 0) {
            pageScrollFunnel?.setViewportHeight(webView.height)
            pageScrollFunnel?.setPageHeight(webView.contentHeight)
            pageScrollFunnel?.logDone()
        }
        pageScrollFunnel = null
    }

    private fun dismissBottomSheet() {
        bottomSheetPresenter.dismiss(childFragmentManager)
        callback()?.onPageDismissBottomSheet()
    }

    private fun updateProgressBar(visible: Boolean) {
        callback()?.onPageUpdateProgressBar(visible)
    }

    private fun startLangLinksActivity() {
        model.title?.let {
            val langIntent = Intent()
            langIntent.setClass(requireActivity(), LangLinksActivity::class.java)
            langIntent.action = LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE
            langIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, it)
            requireActivity().startActivityForResult(langIntent, Constants.ACTIVITY_REQUEST_LANGLINKS)
        }
    }

    private fun trimTabCount() {
        while (app.tabList.size > Constants.MAX_TABS) {
            app.tabList.removeAt(0)
        }
    }

    private fun addTimeSpentReading(timeSpentSec: Int) {
        model.curEntry?.let {
            Completable.fromCallable { AppDatabase.getAppDatabase().historyEntryDao().upsertWithTimeSpent(it, timeSpentSec) }
                .subscribeOn(Schedulers.io())
                .subscribe({}) { L.e(it) }
        }
    }

    private fun getContentTopOffsetParams(context: Context): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DimenUtil.getContentTopOffsetPx(context))
    }

    private fun disableActionTabs(caught: Throwable?) {
        val offline = ThrowableUtil.isOffline(caught)
        for (i in 0 until binding.pageActionsTabLayout.childCount) {
            if (!(offline && PageActionTab.of(i) == PageActionTab.ADD_TO_READING_LIST)) {
                binding.pageActionsTabLayout.disableTab(i)
            }
        }
    }

    private fun setBookmarkIconForPageSavedState(pageSaved: Boolean) {
        binding.pageActionsTabLayout.getChildAt(PageActionTab.ADD_TO_READING_LIST.code())?.let { tab ->
            (tab as MaterialTextView).setCompoundDrawablesWithIntrinsicBounds(null, AppCompatResources.getDrawable(requireContext(),
                if (pageSaved) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp), null, null)
        }
    }

    private fun startTalkTopicActivity(pageTitle: PageTitle) {
        startActivity(TalkTopicsActivity.newIntent(requireActivity(), pageTitle, InvokeSource.PAGE_ACTIVITY))
    }

    private fun startGalleryActivity(fileName: String) {
        if (app.isOnline) {
            bridge.evaluate(JavaScriptActionHandler.getElementAtPosition(DimenUtil.roundedPxToDp(webView.lastTouchX),
                DimenUtil.roundedPxToDp(webView.lastTouchY))) { s ->
                if (!isAdded) {
                    return@evaluate
                }
                var options: ActivityOptionsCompat? = null

                val hitInfo: JavaScriptActionHandler.ImageHitInfo? = JsonUtil.decodeFromString(s)
                hitInfo?.let {
                    val params = CoordinatorLayout.LayoutParams(
                        DimenUtil.roundedDpToPx(it.width),
                        DimenUtil.roundedDpToPx(it.height)
                    )
                    params.topMargin = DimenUtil.roundedDpToPx(it.top)
                    params.leftMargin = DimenUtil.roundedDpToPx(it.left)
                    binding.pageImageTransitionHolder.layoutParams = params
                    binding.pageImageTransitionHolder.visibility = View.VISIBLE
                    ViewUtil.loadImage(binding.pageImageTransitionHolder, it.src)
                    GalleryActivity.setTransitionInfo(it)
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(),
                        binding.pageImageTransitionHolder, getString(R.string.transition_page_gallery))
                }
                webView.post {
                    if (!isAdded) {
                        return@post
                    }
                    model.title?.let {
                        requireActivity().startActivityForResult(GalleryActivity.newIntent(requireActivity(), it, fileName, it.wikiSite, revision,
                                GalleryFunnel.SOURCE_NON_LEAD_IMAGE), Constants.ACTIVITY_REQUEST_GALLERY, options?.toBundle())
                    }
                }
            }
        } else {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.gallery_not_available_offline_snackbar), FeedbackUtil.LENGTH_DEFAULT)
            snackbar.setAction(R.string.gallery_not_available_offline_snackbar_dismiss) { snackbar.dismiss() }
            snackbar.show()
        }
    }

    private fun hidePageContent() {
        leadImagesHandler.hide()
        bridge.loadBlankPage()
        webView.visibility = View.INVISIBLE
    }

    private fun showWatchlistSnackbar(expiry: WatchlistExpiry, watch: Watch) {
        title?.let {
            model.isWatched = watch.watched
            model.hasWatchlistExpiry = expiry !== WatchlistExpiry.NEVER
            if (watch.unwatched) {
                FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, it.displayText))
            } else if (watch.watched) {
                val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                    it.displayText, getString(expiry.stringId)), FeedbackUtil.LENGTH_DEFAULT)
                if (!watchlistExpiryChanged) {
                    snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) {
                        watchlistExpiryChanged = true
                        bottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(expiry))
                    }
                }
                snackbar.show()
            }
        }
    }

    private fun maybeShowAnnouncement() {
        title?.let {
            if (Prefs.hasVisitedArticlePage) {
                disposables.add(ServiceFactory.getRest(it.wikiSite).announcements
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ list ->
                        val country = GeoUtil.geoIPCountry
                        val now = Date()
                        for (announcement in list.items) {
                            if (AnnouncementClient.shouldShow(announcement, country, now) &&
                                announcement.placement == Announcement.PLACEMENT_ARTICLE &&
                                !Prefs.announcementShownDialogs.contains(announcement.id)) {
                                val dialog = AnnouncementDialog(requireActivity(), announcement)
                                dialog.setCancelable(false)
                                dialog.show()
                                break
                            }
                        }
                    }) { caught -> L.d(caught) })
            }
        }
    }

    private fun showFindReferenceInPage(referenceAnchor: String,
                                        backLinksList: List<String?>,
                                        referenceText: String) {
        model.page?.run {
            startSupportActionMode(object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    val menuItem = menu.add(R.string.menu_page_find_in_page)
                    menuItem.actionProvider = FindReferenceInPageActionProvider(requireContext(), referenceAnchor, referenceText, backLinksList)
                    menuItem.expandActionView()
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    mode.tag = "actionModeFindReferenceInPage"
                    return false
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    return false
                }

                override fun onDestroyActionMode(mode: ActionMode) {}
            })
        }
    }

    private fun initPageScrollFunnel() {
        model.page?.run {
            pageScrollFunnel = PageScrollFunnel(app, pageProperties.pageId)
        }
    }

    private fun setupMessageHandlers() {
        linkHandler = object : LinkHandler(requireActivity()) {
            override fun onPageLinkClicked(anchor: String, linkText: String) {
                dismissBottomSheet()
                bridge.execute(JavaScriptActionHandler.prepareToScrollTo(anchor, true))
            }

            override fun onInternalLinkClicked(title: PageTitle) {
                handleInternalLink(title)
            }

            override fun onMediaLinkClicked(title: PageTitle) {
                startGalleryActivity(title.prefixedText)
            }

            override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
                startActivity(ArticleEditDetailsActivity.newIntent(requireContext(), title.displayText, revisionId, title.wikiSite.languageCode))
            }

            // ignore
            override var wikiSite: WikiSite
                get() = model.title?.run { wikiSite } ?: WikiSite.forLanguageCode("en")
                set(wikiSite) {
                    // ignore
                }
        }
        bridge.addListener("link", linkHandler)
        bridge.addListener("setup") { _, _ -> onPageSetupEvent() }
        bridge.addListener("final_setup") { _, _ ->
            if (!isAdded) {
                return@addListener
            }
            bridge.onPcsReady()
            callback()?.onPageLoadComplete()

            // do we have a URL fragment to scroll to?
            model.title?.let { prevTitle ->
                if (!prevTitle.fragment.isNullOrEmpty() && scrollTriggerListener.stagedScrollY == 0) {
                    val scrollDelay = 100
                    webView.postDelayed({
                        if (!isAdded) {
                            return@postDelayed
                        }
                        model.title?.let {
                            if (!it.fragment.isNullOrEmpty()) {
                                scrollToSection(it.fragment!!)
                            }
                        }
                    }, scrollDelay.toLong())
                }
            }
        }
        bridge.addListener("reference") { _, messagePayload ->
            if (!isAdded) {
                return@addListener
            }
            references = JsonUtil.decodeFromString(messagePayload.toString())
            references?.let {
                if (!it.referencesGroup.isNullOrEmpty()) {
                    showBottomSheet(ReferenceDialog())
                }
            }
        }
        bridge.addListener("back_link") { _, messagePayload ->
            messagePayload?.let { payload ->
                val backLinks = payload["backLinks"]?.jsonArray
                if (backLinks != null && !backLinks.isEmpty()) {
                    val backLinksList = backLinks.map { it.jsonObject["id"]?.jsonPrimitive?.content }
                    showFindReferenceInPage(payload["referenceId"]?.jsonPrimitive?.content.orEmpty(), backLinksList, payload["referenceText"]?.jsonPrimitive?.content.orEmpty())
                }
            }
        }
        bridge.addListener("scroll_to_anchor") { _, messagePayload ->
            messagePayload?.let { payload ->
                payload["rect"]?.jsonObject?.let {
                    val diffY = if (it.containsKey("y")) DimenUtil.roundedDpToPx(it["y"]!!.jsonPrimitive.float) else DimenUtil.roundedDpToPx(it["top"]!!.jsonPrimitive.float)
                    val offsetFraction = 3
                    webView.scrollY = webView.scrollY + diffY - webView.height / offsetFraction
                }
            }
        }
        bridge.addListener("image") { _, messagePayload ->
            messagePayload?.let { payload ->
                linkHandler.onUrlClick(UriUtil.decodeURL(payload["href"]?.jsonPrimitive?.content.orEmpty()), payload["title"]?.jsonPrimitive?.content, "")
            }
        }
        bridge.addListener("media") { _, messagePayload ->
            messagePayload?.let { payload ->
                linkHandler.onUrlClick(UriUtil.decodeURL(payload["href"]?.jsonPrimitive?.content.orEmpty()), payload["title"]?.jsonPrimitive?.content, "")
            }
        }
        bridge.addListener("pronunciation") { _, messagePayload ->
            messagePayload?.let { payload ->
                if (avPlayer == null) {
                    avPlayer = AvPlayer()
                }
                if (avCallback == null) {
                    avCallback = AvCallback()
                }
                if (!avPlayer!!.isPlaying && payload.containsKey("url")) {
                    updateProgressBar(true)
                    avPlayer!!.play(UriUtil.resolveProtocolRelativeUrl(payload["url"]?.jsonPrimitive?.content.orEmpty()), avCallback!!)
                } else {
                    updateProgressBar(false)
                    avPlayer!!.stop()
                }
            }
        }
        bridge.addListener("footer_item") { _, messagePayload ->
            messagePayload?.let { payload ->
                when (payload["itemType"]?.jsonPrimitive?.content) {
                    "talkPage" -> model.title?.run { startTalkTopicActivity(this) }
                    "languages" -> startLangLinksActivity()
                    "lastEdited" -> {
                        model.title?.run {
                            loadPage(PageTitle("Special:History/$prefixedText", wikiSite), HistoryEntry(this, HistoryEntry.SOURCE_INTERNAL_LINK))
                        }
                    }
                    "coordinate" -> {
                        model.page?.let { page ->
                            page.pageProperties.geo?.let { geo ->
                                GeoUtil.sendGeoIntent(requireActivity(), geo, page.displayTitle)
                            }
                        }
                    }
                    "disambiguation" -> {
                        // TODO
                        // messagePayload contains an array of URLs called "payload".
                    }
                }
            }
        }
        bridge.addListener("read_more_titles_retrieved") { _, _ -> }
        bridge.addListener("view_license") { _, _ ->
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.cc_by_sa_3_url)))
        }
        bridge.addListener("view_in_browser") { _, _ ->
            model.title?.let {
                UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(it.uri))
            }
        }
    }

    fun reloadFromBackstack() {
        pageFragmentLoadState.setTab(currentTab)
        if (!pageFragmentLoadState.backStackEmpty()) {
            pageFragmentLoadState.loadFromBackStack()
        } else {
            callback()?.onPageLoadMainPageInForegroundTab()
        }
    }

    fun updateInsets(insets: Insets) {
        val swipeOffset = DimenUtil.getContentTopOffsetPx(requireActivity()) + insets.top + REFRESH_SPINNER_ADDITIONAL_OFFSET
        binding.pageRefreshContainer.setProgressViewOffset(false, -swipeOffset, swipeOffset)
    }

    fun onPageMetadataLoaded() {
        updateBookmarkAndMenuOptions()
        if (model.page == null) {
            return
        }
        editHandler.setPage(model.page)
        binding.pageRefreshContainer.isEnabled = true
        binding.pageRefreshContainer.isRefreshing = false
        requireActivity().invalidateOptionsMenu()
        initPageScrollFunnel()
        model.readingListPage?.let { page ->
            model.title?.let { title ->
                disposables.add(Completable.fromAction {
                    page.thumbUrl.equals(title.thumbUrl, true)
                    if (!page.thumbUrl.equals(title.thumbUrl, true) || !page.description.equals(title.description, true)) {
                        AppDatabase.getAppDatabase().readingListPageDao().updateMetadataByTitle(page, title.description, title.thumbUrl)
                    }
                }.subscribeOn(Schedulers.io()).subscribe())
            }
        }
        if (!errorState) {
            editHandler.setPage(model.page)
            webView.visibility = View.VISIBLE
        }
        maybeShowAnnouncement()
        bridge.onMetadataReady()
        // Explicitly set the top margin (even though it might have already been set in the setup
        // handler), since the page metadata might have altered the lead image display state.
        bridge.execute(JavaScriptActionHandler.setTopMargin(leadImagesHandler.topMargin))
        bridge.execute(JavaScriptActionHandler.setFooter(model))
    }

    fun openInNewBackgroundTab(title: PageTitle, entry: HistoryEntry) {
        if (app.tabCount == 0) {
            openInNewTab(title, entry, foregroundTabPosition)
            pageFragmentLoadState.loadFromBackStack()
        } else {
            openInNewTab(title, entry, backgroundTabPosition)
            (requireActivity() as PageActivity).animateTabsButton()
        }
    }

    fun openInNewForegroundTab(title: PageTitle, entry: HistoryEntry) {
        openInNewTab(title, entry, foregroundTabPosition)
        pageFragmentLoadState.loadFromBackStack()
    }

    fun openFromExistingTab(title: PageTitle, entry: HistoryEntry) {
        val selectedTabPosition = app.tabList.firstOrNull { it.backStackPositionTitle != null &&
                it.backStackPositionTitle == title }?.let { app.tabList.indexOf(it) } ?: -1

        if (selectedTabPosition == -1) {
            loadPage(title, entry, pushBackStack = true, squashBackstack = false)
            return
        }
        setCurrentTabAndReset(selectedTabPosition)
    }

    fun loadPage(title: PageTitle, entry: HistoryEntry, pushBackStack: Boolean, squashBackstack: Boolean, isRefresh: Boolean = false) {
        // is the new title the same as what's already being displayed?
        if (currentTab.backStack.isNotEmpty() && currentTab.backStack[currentTab.backStackPosition].title == title) {
            if (model.page == null || isRefresh) {
                pageFragmentLoadState.loadFromBackStack(isRefresh)
            } else if (!title.fragment.isNullOrEmpty()) {
                scrollToSection(title.fragment!!)
            }
            return
        }
        if (squashBackstack) {
            if (app.tabCount > 0) {
                app.tabList[app.tabList.size - 1].clearBackstack()
            }
        }
        loadPage(title, entry, pushBackStack, 0, isRefresh)
    }

    fun loadPage(title: PageTitle, entry: HistoryEntry, pushBackStack: Boolean, stagedScrollY: Int, isRefresh: Boolean = false) {
        // clear the title in case the previous page load had failed.
        clearActivityActionBarTitle()

        if (AccountUtil.isLoggedIn) {
            // explicitly check notifications for the current user
            PollNotificationWorker.schedulePollNotificationJob(requireContext())
        }

        // update the time spent reading of the current page, before loading the new one
        addTimeSpentReading(activeTimer.elapsedSec)
        activeTimer.reset()
        callback()?.onPageSetToolbarElevationEnabled(false)
        tocHandler.setEnabled(false)
        errorState = false
        binding.pageError.visibility = View.GONE
        watchlistExpiryChanged = false
        model.title = title
        model.curEntry = entry
        model.page = null
        model.readingListPage = null
        model.forceNetwork = isRefresh
        webView.visibility = View.VISIBLE
        binding.pageActionsTabLayout.visibility = View.VISIBLE
        binding.pageActionsTabLayout.enableAllTabs()
        updateProgressBar(true)
        pageRefreshed = isRefresh
        references = null
        revision = 0
        closePageScrollFunnel()
        pageFragmentLoadState.load(pushBackStack)
        scrollTriggerListener.stagedScrollY = stagedScrollY
    }

    fun updateFontSize() {
        webView.settings.defaultFontSize = app.getFontSize(requireActivity().window).toInt()
    }

    fun updateBookmarkAndMenuOptions() {
        if (!isAdded) {
            return
        }
        pageActionTabsCallback.updateBookmark(model.isInReadingList)
        val buttonsEnabled = model.page != null && !model.shouldLoadAsMobileWeb
        setBottomBarButtonEnabled(PageActionTab.ADD_TO_READING_LIST, buttonsEnabled)
        setBottomBarButtonEnabled(PageActionTab.CHOOSE_LANGUAGE, buttonsEnabled)
        setBottomBarButtonEnabled(PageActionTab.FONT_AND_THEME, buttonsEnabled)
        setBottomBarButtonEnabled(PageActionTab.VIEW_TOC, buttonsEnabled)
        tocHandler.setEnabled(false)
        requireActivity().invalidateOptionsMenu()
    }

    fun updateBookmarkAndMenuOptionsFromDao() {
        title?.let {
            disposables.add(
                Completable.fromAction { model.readingListPage = AppDatabase.getAppDatabase().readingListPageDao().findPageInAnyList(it) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate {
                        pageActionTabsCallback.updateBookmark(model.readingListPage != null)
                        requireActivity().invalidateOptionsMenu()
                    }
                    .subscribe())
        }
    }

    fun onActionModeShown(mode: ActionMode) {
        // make sure we have a page loaded, since shareHandler makes references to it.
        model.page?.run {
            shareHandler.onTextSelected(mode)
        }
    }

    fun sharePageLink() {
        model.title?.let {
            ShareUtil.shareText(requireActivity(), it)
        }
    }

    fun showFindInPage() {
        model.title?.let { title ->
            bridge.evaluate(JavaScriptActionHandler.expandCollapsedTables(true)) {
                if (!isAdded) {
                    return@evaluate
                }
                val funnel = FindInPageFunnel(app, title.wikiSite, model.page?.run { pageProperties.pageId } ?: -1)
                val findInPageActionProvider = FindInWebPageActionProvider(this, funnel)
                startSupportActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        val menuItem = menu.add(R.string.menu_page_find_in_page)
                        menuItem.actionProvider = findInPageActionProvider
                        menuItem.expandActionView()
                        callback()?.onPageSetToolbarElevationEnabled(false)
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                        mode.tag = "actionModeFindInPage"
                        return false
                    }

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        return false
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {
                        if (!isAdded) {
                            return
                        }
                        funnel.pageHeight = webView.contentHeight
                        funnel.logDone()
                        webView.clearMatches()
                        callback()?.onPageHideSoftKeyboard()
                        callback()?.onPageSetToolbarElevationEnabled(true)
                    }
                })
            }
        }
    }

    fun scrollToSection(sectionAnchor: String) {
        if (!isAdded) {
            return
        }
        bridge.execute(JavaScriptActionHandler.prepareToScrollTo(sectionAnchor, false))
    }

    fun onPageLoadError(caught: Throwable) {
        if (!isAdded) {
            return
        }
        title?.let {
            updateProgressBar(false)
            binding.pageRefreshContainer.isRefreshing = false
            if (pageRefreshed) {
                pageRefreshed = false
            }
            hidePageContent()
            bridge.onMetadataReady()
            if (binding.pageError.visibility != View.VISIBLE) {
                binding.pageError.setError(caught, it)
            }
            binding.pageError.visibility = View.VISIBLE
            binding.pageError.contentTopOffset.layoutParams = getContentTopOffsetParams(requireContext())
            binding.pageError.contentTopOffset.visibility = View.VISIBLE
            binding.pageError.tabLayoutOffset.layoutParams = tabLayoutOffsetParams
            binding.pageError.tabLayoutOffset.visibility = View.VISIBLE
            disableActionTabs(caught)
            binding.pageRefreshContainer.isEnabled = !ThrowableUtil.is404(caught)
            errorState = true
            callback()?.onPageLoadError(it)
        }
    }

    fun refreshPage(stagedScrollY: Int = 0) {
        if (bridge.isLoading) {
            binding.pageRefreshContainer.isRefreshing = false
            return
        }
        model.title?.let { title ->
            model.curEntry?.let { entry ->
                binding.pageError.visibility = View.GONE
                binding.pageActionsTabLayout.enableAllTabs()
                errorState = false
                model.curEntry = HistoryEntry(title, HistoryEntry.SOURCE_HISTORY)
                loadPage(title, entry, false, stagedScrollY, app.isOnline)
            }
        }
    }

    fun clearActivityActionBarTitle() {
        val currentActivity = requireActivity()
        if (currentActivity is PageActivity) {
            currentActivity.clearActionBarTitle()
        }
    }

    fun verifyBeforeEditingDescription(text: String?) {
        page?.let {
            if (!AccountUtil.isLoggedIn && Prefs.totalAnonDescriptionsEdited >= resources.getInteger(R.integer.description_max_anon_edits)) {
                AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.description_edit_anon_limit)
                    .setPositiveButton(R.string.page_editing_login) { _, _ ->
                        startActivity(LoginActivity.newIntent(requireContext(), LoginFunnel.SOURCE_EDIT))
                    }
                    .setNegativeButton(R.string.description_edit_login_cancel_button_text, null)
                    .show()
            } else {
                startDescriptionEditActivity(text)
            }
        }
    }

    fun startDescriptionEditActivity(text: String?) {
        if (Prefs.isDescriptionEditTutorialEnabled) {
            requireActivity().startActivityForResult(DescriptionEditTutorialActivity.newIntent(requireContext(), text),
                Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL)
        } else {
            title?.run {
                val sourceSummary = PageSummaryForEdit(prefixedText, wikiSite.languageCode, this, displayText, description, thumbUrl)
                requireActivity().startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), this, text, sourceSummary, null,
                        DescriptionEditActivity.Action.ADD_DESCRIPTION, InvokeSource.PAGE_ACTIVITY), Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT)
            }
        }
    }

    fun goForward() {
        pageFragmentLoadState.goForward()
    }

    fun showBottomSheet(dialog: BottomSheetDialogFragment) {
        bottomSheetPresenter.show(childFragmentManager, dialog)
    }

    fun loadPage(title: PageTitle, entry: HistoryEntry) {
        callback()?.onPageLoadPage(title, entry)
    }

    fun startSupportActionMode(actionModeCallback: ActionMode.Callback) {
        callback()?.onPageStartSupportActionMode(actionModeCallback)
    }

    fun addToReadingList(title: PageTitle, source: InvokeSource, addToDefault: Boolean) {
        if (addToDefault) {
            var finalPageTitle = title
            // Make sure handle redirected title before saving into database
            disposables.add(ServiceFactory.getRest(title.wikiSite).getSummary(null, title.prefixedText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), finalPageTitle, source) { readingListId ->
                        moveToReadingList(readingListId, finalPageTitle, source, false) }
                }
                .subscribe({
                    finalPageTitle = it.getPageTitle(title.wikiSite)
                }, {
                    L.e(it)
                }))
        } else {
            callback()?.onPageAddToReadingList(title, source)
        }
    }

    fun moveToReadingList(sourceReadingListId: Long, title: PageTitle, source: InvokeSource, showDefaultList: Boolean) {
        callback()?.onPageMoveToReadingList(sourceReadingListId, title, source, showDefaultList)
    }

    fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    fun updateWatchlist(expiry: WatchlistExpiry, unwatch: Boolean) {
        title?.let {
            disposables.add(ServiceFactory.get(it.wikiSite).watchToken
                .subscribeOn(Schedulers.io())
                .flatMap { response ->
                    val watchToken = response.query?.watchToken()
                    if (watchToken.isNullOrEmpty()) {
                        throw RuntimeException("Received empty watch token.")
                    }
                    ServiceFactory.get(it.wikiSite).postWatch(if (unwatch) 1 else null, null, it.prefixedText, expiry.expiry, watchToken)
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ watchPostResponse ->
                    watchPostResponse.getFirst()?.let { watch ->
                        // Reset to make the "Change" button visible.
                        if (watchlistExpiryChanged && unwatch) {
                            watchlistExpiryChanged = false
                        }
                        if (unwatch) {
                            watchlistFunnel.logRemoveSuccess()
                        } else {
                            watchlistFunnel.logAddSuccess()
                        }
                        showWatchlistSnackbar(expiry, watch)
                    }
                }) { caught -> L.d(caught) })
        }
    }

    fun showAnonNotification() {
        (requireActivity() as PageActivity).onAnonNotification()
    }

    private inner class PageActionTabCallback : PageActionTab.Callback {
        override fun onAddToReadingListTabSelected() {
            if (model.isInReadingList) {
                LongPressMenu(binding.pageActionsTabLayout, object : LongPressMenu.Callback {
                    override fun onOpenLink(entry: HistoryEntry) { }

                    override fun onOpenInNewTab(entry: HistoryEntry) { }

                    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                        title?.run {
                            addToReadingList(this, InvokeSource.BOOKMARK_BUTTON, addToDefault)
                        }
                    }

                    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                        page?.let { readingListPage ->
                            title?.run {
                                moveToReadingList(readingListPage.listId, this, InvokeSource.BOOKMARK_BUTTON, true)
                            }
                        }
                    }
                }).show(historyEntry)
            } else {
                title?.run {
                    addToReadingList(this, InvokeSource.BOOKMARK_BUTTON, true)
                }
            }
        }

        override fun onFindInPageTabSelected() {
            showFindInPage()
        }

        override fun onChooseLangTabSelected() {
            startLangLinksActivity()
        }

        override fun onFontAndThemeTabSelected() {
            // If we're looking at the top of the article, then scroll down a bit so that at least
            // some of the text is shown.
            if (webView.scrollY < DimenUtil.leadImageHeightForDevice(requireActivity())) {
                scrolledUpForThemeChange = true
                val animDuration = 250
                val anim = ObjectAnimator.ofInt(webView, "scrollY", webView.scrollY, DimenUtil.leadImageHeightForDevice(requireActivity()))
                anim.setDuration(animDuration.toLong()).addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        showBottomSheet(ThemeChooserDialog.newInstance(InvokeSource.PAGE_ACTION_TAB))
                    }
                })
                anim.start()
            } else {
                scrolledUpForThemeChange = false
                showBottomSheet(ThemeChooserDialog.newInstance(InvokeSource.PAGE_ACTION_TAB))
            }
        }

        override fun onViewToCTabSelected() {
            tocHandler.show()
        }

        override fun updateBookmark(pageSaved: Boolean) {
            setBookmarkIconForPageSavedState(pageSaved)
        }
    }

    private inner class AvCallback : AvPlayer.Callback {
        override fun onSuccess() {
            avPlayer?.stop()
            updateProgressBar(false)
        }

        override fun onError() {
            avPlayer?.stop()
            updateProgressBar(false)
        }
    }

    private inner class WebViewScrollTriggerListener : ObservableWebView.OnContentHeightChangedListener {
        var stagedScrollY = 0
        override fun onContentHeightChanged(contentHeight: Int) {
            if (stagedScrollY > 0 && contentHeight * DimenUtil.densityScalar - webView.height > stagedScrollY) {
                webView.scrollY = stagedScrollY
                stagedScrollY = 0
            }
        }
    }

    private inner class FindReferenceInPageActionProvider constructor(context: Context,
                                                                      private val referenceAnchor: String,
                                                                      private val referenceText: String,
                                                                      private val backLinksList: List<String?>) : ActionProvider(context), View.OnClickListener {
        private val binding = GroupFindReferencesInPageBinding.inflate(LayoutInflater.from(context), null, false)
        private var currentPos = 0
        override fun onCreateActionView(): View {
            binding.findInPagePrev.setOnClickListener(this)
            binding.findInPageNext.setOnClickListener(this)
            binding.referenceLabel.setOnClickListener(this)
            binding.closeButton.setOnClickListener(this)
            binding.referenceLabel.text = getString(R.string.reference_list_title).plus(" $referenceText")
            if (backLinksList.isNotEmpty()) {
                scrollTo(0)
            }
            return binding.root
        }

        override fun overridesItemVisibility(): Boolean {
            return true
        }

        override fun onClick(v: View) {
            when (v) {
                binding.findInPagePrev -> {
                    if (backLinksList.isNotEmpty()) {
                        currentPos = if (--currentPos < 0) backLinksList.size - 1 else currentPos
                        scrollTo(currentPos)
                    }
                }
                binding.findInPageNext -> {
                    if (backLinksList.isNotEmpty()) {
                        currentPos = if (++currentPos >= backLinksList.size) 0 else currentPos
                        scrollTo(currentPos)
                    }
                }
                binding.referenceLabel -> {
                    bridge.execute(JavaScriptActionHandler.scrollToAnchor(referenceAnchor))
                    callback()?.onPageCloseActionMode()
                }
                binding.closeButton -> callback()?.onPageCloseActionMode()
            }
        }

        private fun scrollTo(position: Int) {
            backLinksList[position]?.let {
                binding.referenceCount.text = getString(R.string.find_in_page_result, position + 1,
                    if (backLinksList.isEmpty()) 0 else backLinksList.size)
                bridge.execute(JavaScriptActionHandler.prepareToScrollTo(it, true))
            }
        }
    }

    companion object {
        private const val ARG_THEME_CHANGE_SCROLLED = "themeChangeScrolled"
        private val REFRESH_SPINNER_ADDITIONAL_OFFSET = (16 * DimenUtil.densityScalar).toInt()
    }
}
