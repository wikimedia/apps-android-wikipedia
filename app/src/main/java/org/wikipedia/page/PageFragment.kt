package org.wikipedia.page

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.ActionProvider
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.graphics.Insets
import androidx.core.view.forEach
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.wikipedia.BackPressedHandler
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.LongPressHandler
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.eventplatform.ArticleFindInPageInteractionEvent
import org.wikipedia.analytics.eventplatform.ArticleInteractionEvent
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.analytics.metricsplatform.ArticleFindInPageInteraction
import org.wikipedia.analytics.metricsplatform.ArticleToolbarInteraction
import org.wikipedia.auth.AccountUtil
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.categories.CategoryActivity
import org.wikipedia.categories.CategoryDialog
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.FragmentPageBinding
import org.wikipedia.databinding.GroupFindReferencesInPageBinding
import org.wikipedia.dataclient.RestService
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.donate.CampaignCollection
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.dataclient.okhttp.OkHttpWebViewClient
import org.wikipedia.descriptions.DescriptionEditActivity
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.edit.EditHandler
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.json.JsonUtil
import org.wikipedia.login.LoginActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.media.AvPlayer
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.PollNotificationWorker
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.page.campaign.CampaignDialog
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.page.issues.PageIssuesDialog
import org.wikipedia.page.leadimages.LeadImagesHandler
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.references.PageReferences
import org.wikipedia.page.references.ReferenceDialog
import org.wikipedia.page.shareafact.ShareHandler
import org.wikipedia.page.tabs.Tab
import org.wikipedia.places.PlacesActivity
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.theme.ThemeChooserDialog
import org.wikipedia.util.ActiveTimer
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.ThrowableUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.ObservableWebView
import org.wikipedia.views.PageActionOverflowView
import org.wikipedia.views.ViewUtil
import org.wikipedia.watchlist.WatchlistExpiry
import org.wikipedia.watchlist.WatchlistExpiryDialog
import org.wikipedia.wiktionary.WiktionaryDialog
import java.time.Duration
import java.time.Instant

class PageFragment : Fragment(), BackPressedHandler, CommunicationBridge.CommunicationBridgeListener, ThemeChooserDialog.Callback,
    ReferenceDialog.Callback, WiktionaryDialog.Callback, WatchlistExpiryDialog.Callback {

    interface Callback {
        fun onPageDismissBottomSheet()
        fun onPageLoadComplete()
        fun onPageLoadPage(title: PageTitle, entry: HistoryEntry)
        fun onPageInitWebView(v: ObservableWebView)
        fun onPageLoadMainPageInForegroundTab()
        fun onPageUpdateProgressBar(visible: Boolean)
        fun onPageStartSupportActionMode(callback: ActionMode.Callback)
        fun onPageHideSoftKeyboard()
        fun onPageWatchlistExpirySelect(expiry: WatchlistExpiry)
        fun onPageLoadError(title: PageTitle)
        fun onPageLoadErrorBackPressed()
        fun onPageSetToolbarElevationEnabled(enabled: Boolean)
        fun onPageCloseActionMode()
        fun onPageRequestEditSection(sectionId: Int, sectionAnchor: String?, title: PageTitle, highlightText: String?)
        fun onPageRequestLangLinks(title: PageTitle)
        fun onPageRequestGallery(title: PageTitle, fileName: String, wikiSite: WikiSite, revision: Long, isLeadImage: Boolean, options: ActivityOptionsCompat?)
        fun onPageRequestAddImageTags(mwQueryPage: MwQueryPage, invokeSource: InvokeSource)
        fun onPageRequestEditDescription(text: String?, title: PageTitle, sourceSummary: PageSummaryForEdit?,
                                         targetSummary: PageSummaryForEdit?, action: DescriptionEditActivity.Action, invokeSource: InvokeSource)
    }

    private var _binding: FragmentPageBinding? = null
    val binding get() = _binding!!

    private val activeTimer = ActiveTimer()
    private val scrollTriggerListener = WebViewScrollTriggerListener()
    private val pageRefreshListener = OnRefreshListener { refreshPage() }
    private val pageActionItemCallback = PageActionItemCallback()

    private lateinit var bridge: CommunicationBridge
    private lateinit var leadImagesHandler: LeadImagesHandler
    private lateinit var pageFragmentLoadState: PageFragmentLoadState
    private lateinit var bottomBarHideHandler: ViewHideHandler
    internal var articleInteractionEvent: ArticleInteractionEvent? = null
    internal var metricsPlatformArticleEventToolbarInteraction = ArticleToolbarInteraction(this)
    private var pageRefreshed = false
    private var errorState = false
    private var scrolledUpForThemeChange = false
    private var references: PageReferences? = null
    private var avPlayer: AvPlayer? = null
    private var avCallback: AvCallback? = null
    private var sections: MutableList<Section>? = null
    private var app = WikipediaApp.instance

    override lateinit var linkHandler: LinkHandler
    override lateinit var webView: ObservableWebView
    override var model = PageViewModel()
    override val toolbarMargin get() = (requireActivity() as PageActivity).getToolbarMargin()
    override val isPreview get() = false
    override val referencesGroup get() = references?.referencesGroup
    override val selectedReferenceIndex get() = references?.selectedIndex ?: 0

    lateinit var sidePanelHandler: SidePanelHandler
    lateinit var shareHandler: ShareHandler
    lateinit var editHandler: EditHandler
    var revision = 0L

    private val shouldCreateNewTab get() = currentTab.backStack.isNotEmpty()
    private val backgroundTabPosition get() = 0.coerceAtLeast(foregroundTabPosition - 1)
    private val foregroundTabPosition get() = app.tabList.size
    private val tabLayoutOffsetParams get() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, binding.pageActionsTabLayout.height)
    val currentTab get() = app.tabList.last()
    val title get() = model.title
    val page get() = model.page
    val historyEntry get() = model.curEntry
    val containerView get() = binding.pageContentsContainer
    val isLoading get() = bridge.isLoading
    val leadImageEditLang get() = leadImagesHandler.callToActionEditLang

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPageBinding.inflate(inflater, container, false)
        webView = binding.pageWebView
        initWebViewListeners()
        binding.pageRefreshContainer.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.progressive_color))
        binding.pageRefreshContainer.scrollableChild = webView
        binding.pageRefreshContainer.setOnRefreshListener(pageRefreshListener)
        val swipeOffset = DimenUtil.getContentTopOffsetPx(requireActivity()) + REFRESH_SPINNER_ADDITIONAL_OFFSET
        binding.pageRefreshContainer.setProgressViewOffset(false, -swipeOffset, swipeOffset)
        binding.pageActionsTabLayout.callback = pageActionItemCallback
        savedInstanceState?.let {
            scrolledUpForThemeChange = it.getBoolean(ARG_THEME_CHANGE_SCROLLED, false)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        bottomBarHideHandler = ViewHideHandler(binding.pageActionsTabContainer, null, Gravity.BOTTOM, updateElevation = false) { false }
        bottomBarHideHandler.setScrollView(webView)
        bottomBarHideHandler.enabled = Prefs.readingFocusModeEnabled

        webView.addOnScrollChangeListener { _, scrollY, _ ->
            if (scrollY > (DimenUtil.roundedDpToPx(webView.contentHeight.toFloat()) - (DimenUtil.displayHeightPx * 2)) &&
                !model.isReadMoreLoaded) {
                bridge.execute(JavaScriptActionHandler.appendReadMode(model))
                model.isReadMoreLoaded = true
            }
        }

        editHandler = EditHandler(this, bridge)
        sidePanelHandler = SidePanelHandler(this, bridge)
        leadImagesHandler = LeadImagesHandler(this, webView, binding.pageHeaderView, callback())
        shareHandler = ShareHandler(this, bridge)
        pageFragmentLoadState = PageFragmentLoadState(model, this, webView, bridge, leadImagesHandler, currentTab)

        if (callback() != null) {
            LongPressHandler(webView, HistoryEntry.SOURCE_INTERNAL_LINK, PageContainerLongPressHandler(this))
        }

        if (shouldLoadFromBackstack(activity) || savedInstanceState != null) {
            reloadFromBackstack()
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
        sidePanelHandler.log()
        leadImagesHandler.dispose()
        webView.clearAllListeners()
        (webView.parent as ViewGroup).removeView(webView)
        Prefs.isSuggestedEditsHighestPriorityEnabled = false
        _binding = null
        super.onDestroyView()
    }

    override fun onPause() {
        super.onPause()
        bridge.execute(JavaScriptActionHandler.pauseAllMedia())
        if (avPlayer?.isPlaying == true) {
            avPlayer?.stop()
            updateProgressBar(false)
        }
        activeTimer.pause()
        addTimeSpentReading(activeTimer.elapsedSec)
        pageFragmentLoadState.updateCurrentBackStackItem()
        app.commitTabState()
        val time = if (app.tabList.size >= 1 && !pageFragmentLoadState.backStackEmpty()) System.currentTimeMillis() else 0
        Prefs.pageLastShown = time
        articleInteractionEvent?.pause()
        metricsPlatformArticleEventToolbarInteraction.pause()
    }

    override fun onResume() {
        super.onResume()
        activeTimer.resume()
        val params = CoordinatorLayout.LayoutParams(1, 1)
        binding.pageImageTransitionHolder.layoutParams = params
        binding.pageImageTransitionHolder.visibility = View.GONE
        binding.pageActionsTabLayout.update()
        updateQuickActionsAndMenuOptions()
        articleInteractionEvent?.resume()
        metricsPlatformArticleEventToolbarInteraction.resume()
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
        articleInteractionEvent?.logBackClick()
        metricsPlatformArticleEventToolbarInteraction.logBackClick()
        if (sidePanelHandler.isVisible) {
            sidePanelHandler.hide()
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
        // We disable and then re-enable scroll events coming from the WebView, because toggling
        // reading focus mode within the article could actually change the dimensions of the page,
        // which will cause extraneous scroll events to be sent.
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

    override fun onEditingPrefsChanged() { }

    override fun wiktionaryShowDialogForTerm(term: String) {
        shareHandler.showWiktionaryDefinition(term)
    }

    override fun onExpiryChanged(expiry: WatchlistExpiry) {
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
            app.appSessionEvent.touchSession()
        }
        webView.addOnContentHeightChangedListener(scrollTriggerListener)



        webView.addOnScrollChangeListener({ oldScrollY, scrollY, isHumanScroll ->
            LinkPreviewDialog.hide(childFragmentManager)
        })



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
                        bridge.execute(JavaScriptActionHandler.mobileWebChromeShim(DimenUtil.roundedPxToDp(((requireActivity() as AppCompatActivity).supportActionBar?.height ?: 0).toFloat()),
                            DimenUtil.roundedPxToDp(binding.pageActionsTabLayout.height.toFloat())))
                    }
                }
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

                sidePanelHandler.setupForNewPage(page)
                sidePanelHandler.setEnabled(true)
                model.isReadMoreLoaded = false
            }
        }
        bridge.evaluate(JavaScriptActionHandler.getProtection()) { value ->
            if (!isAdded) {
                return@evaluate
            }
            model.page?.let { page ->
                page.pageProperties.protection = JsonUtil.decodeFromString(value)
                updateQuickActionsAndMenuOptions()
            }
        }
    }

    private fun handleInternalLink(title: PageTitle) {
        if (!isResumed) {
            return
        }
        if (title.namespace() === Namespace.USER_TALK || title.namespace() === Namespace.TALK) {
            startTalkTopicsActivity(title)
            return
        } else if (title.namespace() == Namespace.CATEGORY) {
            startActivity(CategoryActivity.newIntent(requireActivity(), title))
            return
        }

        dismissBottomSheet()
        val historyEntry = HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK)

        if (title == model.title && !title.fragment.isNullOrEmpty()) {
            scrollToSection(title.fragment!!)
            return
        }

        model.title?.run {
            historyEntry.referrer = uri
        }
        if (title.namespace() !== Namespace.MAIN || !Prefs.isLinkPreviewEnabled) {
            loadPage(title, historyEntry)
        } else {
            LinkPreviewDialog.show(childFragmentManager, R.id.page_contents_container, historyEntry)
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
            app.tabList.last().squashBackstack()
            pageFragmentLoadState.loadFromBackStack()
        }
    }

    private fun selectedTabPosition(title: PageTitle): Int {
        return app.tabList.firstOrNull { it.backStackPositionTitle != null &&
                title == it.backStackPositionTitle }?.let { app.tabList.indexOf(it) } ?: -1
    }

    private fun openInNewTab(title: PageTitle, entry: HistoryEntry, position: Int) {
        val selectedTabPosition = selectedTabPosition(title)
        if (selectedTabPosition >= 0) {
            setCurrentTabAndReset(selectedTabPosition)
            return
        }
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
                lifecycleScope.launch(CoroutineExceptionHandler { _, t -> L.e(t) }) {
                    ServiceFactory.get(title.wikiSite).getInfoByPageIdsOrTitles(null, title.prefixedText)
                        .query?.firstPage()?.let { page ->
                            WikipediaApp.instance.tabList.find { it.backStackPositionTitle == title }?.backStackPositionTitle?.apply {
                                thumbUrl = page.thumbUrl()
                                description = page.description
                            }
                        }
                }
            }
            requireActivity().invalidateOptionsMenu()
        } else {
            pageFragmentLoadState.setTab(currentTab)
            currentTab.backStack.add(PageBackStackItem(title, entry))
        }
    }

    private fun dismissBottomSheet() {
        ExclusiveBottomSheetPresenter.dismiss(childFragmentManager)
        callback()?.onPageDismissBottomSheet()
    }

    private fun updateProgressBar(visible: Boolean) {
        callback()?.onPageUpdateProgressBar(visible)
    }

    private fun startLangLinksActivity() {
        model.title?.let {
            callback()?.onPageRequestLangLinks(it)
        }
    }

    private fun trimTabCount() {
        while (app.tabList.size > Constants.MAX_TABS) {
            app.tabList.removeAt(0)
        }
    }

    private fun addTimeSpentReading(timeSpentSec: Int) {
        model.curEntry?.let {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable -> L.e(throwable) }) {
                AppDatabase.instance.historyEntryDao().upsertWithTimeSpent(it, timeSpentSec)
            }
        }
    }

    private fun getContentTopOffsetParams(context: Context): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, DimenUtil.getContentTopOffsetPx(context))
    }

    private fun disableActionTabs(caught: Throwable?) {
        val offline = ThrowableUtil.isOffline(caught)
        for (i in 0 until binding.pageActionsTabLayout.childCount) {
            if (!offline) {
                binding.pageActionsTabLayout.disableTab(i)
            }
        }
    }

    private fun startTalkTopicsActivity(title: PageTitle, stripUrlFragment: Boolean = false) {
        val talkTitle = title.copy()
        if (stripUrlFragment) {
            talkTitle.fragment = null
        }
        startActivity(TalkTopicsActivity.newIntent(requireActivity(), talkTitle, InvokeSource.PAGE_ACTIVITY))
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
                        callback()?.onPageRequestGallery(it, fileName, it.wikiSite, revision, false, options)
                    }
                }
            }
        } else {
            val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.gallery_not_available_offline_snackbar))
            snackbar.setAction(R.string.gallery_not_available_offline_snackbar_dismiss) { snackbar.dismiss() }
            snackbar.show()
        }
    }

    private fun hidePageContent() {
        leadImagesHandler.hide()
        bridge.loadBlankPage()
        webView.visibility = View.INVISIBLE
    }

    @Suppress("KotlinConstantConditions")
    private fun showWatchlistSnackbar() {
        title?.let {
            if (!model.isWatched) {
                FeedbackUtil.showMessage(this, getString(R.string.watchlist_page_removed_from_watchlist_snackbar, it.displayText))
            } else if (model.isWatched) {
                val snackbar = FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                    it.displayText, getString(WatchlistExpiry.NEVER.stringId)))
                snackbar.setAction(R.string.watchlist_page_add_to_watchlist_snackbar_action) { _ ->
                    ExclusiveBottomSheetPresenter.show(childFragmentManager, WatchlistExpiryDialog.newInstance(it, WatchlistExpiry.NEVER))
                }
                snackbar.show()
            }
        }
    }

    fun updateWatchlistExpiry(expiry: WatchlistExpiry) {
        model.hasWatchlistExpiry = expiry !== WatchlistExpiry.NEVER
    }

    private fun maybeShowAnnouncement() {
        title?.let { pageTitle ->
            // Check if the pause time is older than 1 day.
            val dateDiff = Duration.between(Instant.ofEpochMilli(Prefs.announcementPauseTime), Instant.now())
            if (Prefs.hasVisitedArticlePage && dateDiff.toDays() >= 1) {
                lifecycleScope.launch(CoroutineExceptionHandler { _, t -> L.e(t) }) {
                    val campaignList = CampaignCollection.getActiveCampaigns()
                    val availableCampaign = campaignList.find { campaign -> campaign.assets[app.appOrSystemLanguageCode] != null }
                    availableCampaign?.let {
                        if (!Prefs.announcementShownDialogs.contains(it.id)) {
                            DonorExperienceEvent.logAction("impression", "article_banner", pageTitle.wikiSite.languageCode, it.id)
                            val dialog = CampaignDialog(requireActivity(), it)
                            dialog.setCancelable(false)
                            dialog.show()
                        }
                    }
                }
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

                override fun onDestroyActionMode(mode: ActionMode) {
                    bridge.execute(JavaScriptActionHandler.removeHighlights())
                }
            })
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
                startActivity(ArticleEditDetailsActivity.newIntent(requireContext(), title, revisionId))
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
            articleInteractionEvent?.logLoaded()
            metricsPlatformArticleEventToolbarInteraction.logLoaded()
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
                if (it.referencesGroup.isNotEmpty()) {
                    showBottomSheet(ReferenceDialog())
                }
            }
        }
        bridge.addListener("back_link") { _, messagePayload ->
            messagePayload?.let { payload ->
                val backLinks = payload["backLinks"]?.jsonArray
                if (!backLinks.isNullOrEmpty()) {
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
                    "talkPage" -> model.title?.run {
                        articleInteractionEvent?.logTalkPageArticleClick()
                        metricsPlatformArticleEventToolbarInteraction.logTalkPageArticleClick()
                        startTalkTopicsActivity(this, true)
                    }
                    "languages" -> startLangLinksActivity()
                    "lastEdited" -> {
                        model.title?.run {
                            articleInteractionEvent?.logEditHistoryArticleClick()
                            metricsPlatformArticleEventToolbarInteraction.logEditHistoryArticleClick()
                            startActivity(EditHistoryListActivity.newIntent(requireContext(), this))
                        }
                    }
                    "coordinate" -> {
                        model.page?.let { page ->
                            val location = page.pageProperties.geo
                            if (location != null) {
                                PlacesEvent.logAction("places_click", "article_footer")
                                requireActivity().startActivity(PlacesActivity.newIntent(requireContext(), page.title, location))
                            } else {
                                FeedbackUtil.showMessage(this@PageFragment, getString(R.string.action_item_view_on_map_unavailable))
                            }
                        }
                    }
                    "pageIssues" -> {
                        val array = payload["payload"]
                        if (array != null && array.jsonArray.isNotEmpty() && model.title != null) {
                            val issues = array.jsonArray.mapNotNull {
                                it.jsonObject["html"]?.jsonPrimitive?.content
                            }
                            PageIssuesDialog(requireActivity(), model.title!!.wikiSite, issues) { url, title, linkText ->
                                linkHandler.onUrlClick(url, title, linkText)
                            }
                                .setTitle(R.string.page_issues_title)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
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
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(getString(R.string.cc_by_sa_4_url)))
        }
        bridge.addListener("view_in_browser") { _, _ ->
            model.title?.let {
                UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(it.uri))
            }
        }
    }

    fun reloadFromBackstack(forceReload: Boolean = true) {
        if (pageFragmentLoadState.setTab(currentTab) || forceReload) {
            if (!pageFragmentLoadState.backStackEmpty()) {
                pageFragmentLoadState.loadFromBackStack()
            } else {
                callback()?.onPageLoadMainPageInForegroundTab()
            }
        }
    }

    fun updateInsets(insets: Insets) {
        val swipeOffset = DimenUtil.getContentTopOffsetPx(requireActivity()) + insets.top + REFRESH_SPINNER_ADDITIONAL_OFFSET
        binding.pageRefreshContainer.setProgressViewOffset(false, -swipeOffset, swipeOffset)
    }

    fun onPageMetadataLoaded(redirectedFrom: String? = null) {
        updateQuickActionsAndMenuOptions()
        if (model.page == null) {
            return
        }
        model.page?.run {
            articleInteractionEvent = ArticleInteractionEvent(model.title?.wikiSite?.dbName()!!, pageProperties.pageId)
        }
        editHandler.setPage(model.page)
        binding.pageRefreshContainer.isEnabled = true
        binding.pageRefreshContainer.isRefreshing = false
        requireActivity().invalidateOptionsMenu()
        redirectedFrom?.let {
            FeedbackUtil.showMessage(requireActivity(), getString(R.string.redirected_from_snackbar, it), Snackbar.LENGTH_SHORT)
        }
        model.readingListPage?.let { page ->
            model.title?.let { title ->
                lifecycleScope.launch(CoroutineExceptionHandler { _, t ->
                    L.e(t)
                }) {
                    if (!page.thumbUrl.equals(title.thumbUrl, true) || !page.description.equals(title.description, true)) {
                        AppDatabase.instance.readingListPageDao().updateMetadataByTitle(page, title.description, title.thumbUrl)
                    }
                }
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
        val selectedTabPosition = selectedTabPosition(title)

        if (selectedTabPosition == -1) {
            loadPage(title, entry, pushBackStack = true, squashBackstack = false)
            return
        }
        setCurrentTabAndReset(selectedTabPosition)
    }

    fun loadPage(title: PageTitle, entry: HistoryEntry, pushBackStack: Boolean, squashBackstack: Boolean, isRefresh: Boolean = false) {
        // is the new title the same as what's already being displayed?
        if (currentTab.backStack.isNotEmpty() &&
                title == currentTab.backStack[currentTab.backStackPosition].title) {
            if (model.page == null || isRefresh) {
                pageFragmentLoadState.loadFromBackStack(isRefresh)
            } else if (!title.fragment.isNullOrEmpty()) {
                scrollToSection(title.fragment!!)
            }
            return
        }
        if (squashBackstack) {
            if (app.tabCount > 0) {
                app.tabList.last().clearBackstack()
            }
        }
        loadPage(title, entry, pushBackStack, 0, isRefresh)
    }

    fun loadPage(title: PageTitle, entry: HistoryEntry, pushBackStack: Boolean, stagedScrollY: Int, isRefresh: Boolean = false) {
        // clear the title in case the previous page load had failed.
        clearActivityActionBarTitle()

        if (ExclusiveBottomSheetPresenter.getCurrentBottomSheet(childFragmentManager) !is ThemeChooserDialog) {
            dismissBottomSheet()
        }

        if (AccountUtil.isLoggedIn) {
            // explicitly check notifications for the current user
            PollNotificationWorker.schedulePollNotificationJob(requireContext())
        }

        EventPlatformClient.AssociationController.beginNewPageView()

        // update the time spent reading of the current page, before loading the new one
        addTimeSpentReading(activeTimer.elapsedSec)
        activeTimer.reset()
        callback()?.onPageSetToolbarElevationEnabled(false)
        sidePanelHandler.setEnabled(false)
        errorState = false
        binding.pageError.visibility = View.GONE
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
        pageFragmentLoadState.load(pushBackStack)
        scrollTriggerListener.stagedScrollY = stagedScrollY
    }

    fun updateFontSize() {
        webView.settings.defaultFontSize = app.getFontSize().toInt()
    }

    fun updateQuickActionsAndMenuOptions() {
        if (!isAdded) {
            return
        }
        binding.pageActionsTabLayout.forEach { it as MaterialTextView
            val pageActionItem = PageActionItem.find(it.id)
            var enabled = model.page != null && (!model.shouldLoadAsMobileWeb || (model.shouldLoadAsMobileWeb && pageActionItem.isAvailableOnMobileWeb))
            when (pageActionItem) {
                PageActionItem.ADD_TO_WATCHLIST -> {
                    it.setText(if (model.isWatched) R.string.menu_page_unwatch else R.string.menu_page_watch)
                    it.setCompoundDrawablesWithIntrinsicBounds(0, PageActionItem.watchlistIcon(model.isWatched, model.hasWatchlistExpiry), 0, 0)
                    enabled = enabled && AccountUtil.isLoggedIn
                }
                PageActionItem.SAVE -> {
                    it.setCompoundDrawablesWithIntrinsicBounds(0, PageActionItem.readingListIcon(model.isInReadingList), 0, 0)
                }
                PageActionItem.EDIT_ARTICLE -> {
                    it.setCompoundDrawablesWithIntrinsicBounds(0, PageActionItem.editArticleIcon(model.page?.pageProperties?.canEdit != true), 0, 0)
                }
                PageActionItem.VIEW_ON_MAP -> {
                    val geoAvailable = model.page?.pageProperties?.geo != null
                    val tintColor = ResourceUtil.getThemedColorStateList(requireContext(), if (geoAvailable) R.attr.primary_color else R.attr.inactive_color)
                    it.setTextColor(tintColor)
                    TextViewCompat.setCompoundDrawableTintList(it, tintColor)
                }
                else -> { }
            }
            it.isEnabled = enabled
            it.alpha = if (enabled) 1f else 0.5f
        }
        sidePanelHandler.setEnabled(false)
        requireActivity().invalidateOptionsMenu()
    }

    fun updateBookmarkAndMenuOptionsFromDao() {
        title?.let {
            lifecycleScope.launch {
                model.readingListPage = AppDatabase.instance.readingListPageDao().findPageInAnyList(it)
                updateQuickActionsAndMenuOptions()
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    fun onActionModeShown(mode: ActionMode) {
        // make sure we have a page loaded, since shareHandler makes references to it.
        model.page?.run {
            shareHandler.onTextSelected(mode)
        }
    }

    fun onRequestEditSection(sectionId: Int, sectionAnchor: String?, title: PageTitle, highlightText: String?) {
        callback()?.onPageRequestEditSection(sectionId, sectionAnchor, title, highlightText)
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
                val articleFindInPageInteractionEvent = ArticleFindInPageInteractionEvent(model.page?.pageProperties?.pageId ?: -1)
                val articleFindInPageInteractionEventMetricsPlatform = ArticleFindInPageInteraction(this)
                val findInPageActionProvider = FindInWebPageActionProvider(this, articleFindInPageInteractionEvent, articleFindInPageInteractionEventMetricsPlatform)
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
                        articleFindInPageInteractionEvent.pageHeight = webView.contentHeight
                        articleFindInPageInteractionEvent.logDone()
                        articleFindInPageInteractionEventMetricsPlatform.pageHeight = webView.contentHeight
                        articleFindInPageInteractionEventMetricsPlatform.logDone()
                        webView.clearMatches()
                        callback()?.onPageHideSoftKeyboard()
                        callback()?.onPageSetToolbarElevationEnabled(true)
                    }
                })
            }
        }
    }

    private fun scrollToSection(sectionAnchor: String) {
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

   private fun clearActivityActionBarTitle() {
        val currentActivity = requireActivity()
        if (currentActivity is PageActivity) {
            currentActivity.clearActionBarTitle()
        }
   }

    fun verifyBeforeEditingDescription(text: String?, invokeSource: InvokeSource) {
        page?.let {
            if (!AccountUtil.isLoggedIn && Prefs.totalAnonDescriptionsEdited >= resources.getInteger(R.integer.description_max_anon_edits)) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setMessage(R.string.description_edit_anon_limit)
                    .setPositiveButton(R.string.page_editing_login) { _, _ ->
                        startActivity(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_EDIT))
                    }
                    .setNegativeButton(R.string.description_edit_login_cancel_button_text, null)
                    .show()
            } else {
                startDescriptionEditActivity(text, invokeSource)
            }
        }
    }

    private fun startDescriptionEditActivity(text: String?, invokeSource: InvokeSource) {
        title?.run {
            val sourceSummary = PageSummaryForEdit(prefixedText, wikiSite.languageCode, this,
                displayText, description, thumbUrl)
            callback()?.onPageRequestEditDescription(text, this, sourceSummary, null,
                DescriptionEditActivity.Action.ADD_DESCRIPTION, invokeSource)
        }
    }

    fun goForward() {
        pageFragmentLoadState.goForward()
    }

    fun showBottomSheet(dialog: BottomSheetDialogFragment) {
        ExclusiveBottomSheetPresenter.show(childFragmentManager, dialog)
    }

    fun loadPage(title: PageTitle, entry: HistoryEntry) {
        callback()?.onPageLoadPage(title, entry)
    }

    fun startSupportActionMode(actionModeCallback: ActionMode.Callback) {
        callback()?.onPageStartSupportActionMode(actionModeCallback)
    }

    fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    fun updateWatchlist() {
        title?.let {
            lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
                L.d(throwable)
            }) {
                val token = ServiceFactory.get(it.wikiSite).getWatchToken().query?.watchToken() ?: throw RuntimeException("Received empty watch token.")
                val watch = ServiceFactory.get(it.wikiSite).watch(if (model.isWatched) 1 else null, null, it.prefixedText, WatchlistExpiry.NEVER.expiry, token)
                watch.getFirst()?.let { firstWatch ->
                    if (model.isWatched) {
                        WatchlistAnalyticsHelper.logRemovedFromWatchlistSuccess(it, requireContext())
                    } else {
                        WatchlistAnalyticsHelper.logAddedToWatchlistSuccess(it, requireContext())
                    }
                    model.isWatched = firstWatch.watched
                    updateWatchlistExpiry(WatchlistExpiry.NEVER)
                    showWatchlistSnackbar()
                }
                updateQuickActionsAndMenuOptions()
            }
        }
    }

    fun showAnonNotification() {
        (requireActivity() as PageActivity).onAnonNotification()
    }

    fun showOverflowMenu(anchor: View) {
        PageActionOverflowView(requireContext()).show(anchor, pageActionItemCallback, currentTab, model)
    }

    fun goToMainTab() {
        startActivity(MainActivity.newIntent(requireContext())
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Constants.INTENT_RETURN_TO_MAIN, true)
            .putExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()))
        requireActivity().finish()
    }

    private inner class AvCallback : AvPlayer.Callback {
        override fun onSuccess() {
            avPlayer?.stop()
            updateProgressBar(false)
        }

        override fun onError(code: Int, extra: Int) {
            if (avPlayer?.isPlaying == true) {
                avPlayer?.stop()
            }
            FeedbackUtil.showMessage(this@PageFragment, R.string.media_playback_error)
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

    inner class PageActionItemCallback : PageActionItem.Callback {
        override fun onSaveSelected() {
            if (model.isInReadingList) {
                val anchor = if (Prefs.customizeToolbarOrder.contains(PageActionItem.SAVE.id))
                    binding.pageActionsTabLayout else (requireActivity() as PageActivity).getOverflowMenu()
                LongPressMenu(anchor, existsInAnyList = false, callback = object : LongPressMenu.Callback {
                    override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                        title?.run {
                            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), this, addToDefault, InvokeSource.BOOKMARK_BUTTON)
                        }
                    }

                    override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                        page?.let { readingListPage ->
                            title?.run {
                                ReadingListBehaviorsUtil.moveToList(requireActivity(), readingListPage.listId, this, InvokeSource.BOOKMARK_BUTTON)
                            }
                        }
                    }
                }).show(historyEntry)
            } else {
                title?.run {
                    ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), this, true, InvokeSource.BOOKMARK_BUTTON)
                }
            }
            articleInteractionEvent?.logSaveClick()
            metricsPlatformArticleEventToolbarInteraction.logSaveClick()
        }

        override fun onLanguageSelected() {
            startLangLinksActivity()
            articleInteractionEvent?.logLanguageClick()
            metricsPlatformArticleEventToolbarInteraction.logLanguageClick()
        }

        override fun onFindInArticleSelected() {
            showFindInPage()
            articleInteractionEvent?.logFindInArticleClick()
            metricsPlatformArticleEventToolbarInteraction.logFindInArticleClick()
        }

        override fun onThemeSelected() {
            articleInteractionEvent?.logThemeClick()
            metricsPlatformArticleEventToolbarInteraction.logThemeClick()

            // If we're looking at the top of the article, then scroll down a bit so that at least
            // some of the text is shown.
            if (webView.scrollY < DimenUtil.leadImageHeightForDevice(requireActivity())) {
                scrolledUpForThemeChange = true
                val animDuration = 250
                val anim = ObjectAnimator.ofInt(webView, "scrollY", webView.scrollY, DimenUtil.leadImageHeightForDevice(requireActivity()))
                anim.setDuration(animDuration.toLong()).doOnEnd {
                    showBottomSheet(ThemeChooserDialog.newInstance(InvokeSource.PAGE_ACTION_TAB))
                }
                anim.start()
            } else {
                scrolledUpForThemeChange = false
                showBottomSheet(ThemeChooserDialog.newInstance(InvokeSource.PAGE_ACTION_TAB))
            }
        }

        override fun onContentsSelected() {
            sidePanelHandler.showToC()
            articleInteractionEvent?.logContentsClick()
            metricsPlatformArticleEventToolbarInteraction.logContentsClick()
        }

        override fun onShareSelected() {
            sharePageLink()
            articleInteractionEvent?.logShareClick()
            metricsPlatformArticleEventToolbarInteraction.logShareClick()
        }

        override fun onAddToWatchlistSelected() {
            if (model.isWatched) {
                WatchlistAnalyticsHelper.logRemovedFromWatchlist(model.title, requireContext())
                articleInteractionEvent?.logUnWatchClick()
                metricsPlatformArticleEventToolbarInteraction.logUnWatchClick()
            } else {
                WatchlistAnalyticsHelper.logAddedToWatchlist(model.title, requireContext())
                articleInteractionEvent?.logWatchClick()
                metricsPlatformArticleEventToolbarInteraction.logWatchClick()
            }
            updateWatchlist()
        }

        override fun onViewTalkPageSelected() {
            title?.let {
                startTalkTopicsActivity(it, true)
            }
            articleInteractionEvent?.logTalkPageClick()
            metricsPlatformArticleEventToolbarInteraction.logTalkPageClick()
        }

        override fun onViewEditHistorySelected() {
            title?.run {
                startActivity(EditHistoryListActivity.newIntent(requireContext(), this))
            }
            articleInteractionEvent?.logEditHistoryClick()
            metricsPlatformArticleEventToolbarInteraction.logEditHistoryClick()
        }

        override fun onNewTabSelected() {
            startActivity(PageActivity.newIntentForNewTab(requireContext()))
            articleInteractionEvent?.logNewTabClick()
            metricsPlatformArticleEventToolbarInteraction.logNewTabClick()
        }

        override fun onExploreSelected() {
            goToMainTab()
            articleInteractionEvent?.logExploreClick()
            metricsPlatformArticleEventToolbarInteraction.logExploreClick()
        }

        override fun onCategoriesSelected() {
            title?.let {
                ExclusiveBottomSheetPresenter.show(childFragmentManager, CategoryDialog.newInstance(it))
            }
            articleInteractionEvent?.logCategoriesClick()
            metricsPlatformArticleEventToolbarInteraction.logCategoriesClick()
        }

        override fun onEditArticleSelected() {
            editHandler.startEditingArticle()
            articleInteractionEvent?.logEditArticleClick()
            metricsPlatformArticleEventToolbarInteraction.logEditArticleClick()
        }

        override fun onViewOnMapSelected() {
            title?.let {
                val location = page?.pageProperties?.geo
                if (location != null) {
                    PlacesEvent.logAction("places_click", "article_more_menu")
                    requireActivity().startActivity(PlacesActivity.newIntent(requireContext(), it, location))
                } else {
                    FeedbackUtil.showMessage(this@PageFragment, getString(R.string.action_item_view_on_map_unavailable))
                }
            }
        }

        override fun forwardClick() {
            goForward()
            articleInteractionEvent?.logForwardClick()
            metricsPlatformArticleEventToolbarInteraction.logForwardClick()
        }
    }

    companion object {
        private const val ARG_THEME_CHANGE_SCROLLED = "themeChangeScrolled"
        private val REFRESH_SPINNER_ADDITIONAL_OFFSET = (16 * DimenUtil.densityScalar).toInt()
    }
}
