package org.wikipedia.main

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.ListFormatter
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.descendants
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.BackPressedHandler
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.activitytab.ActivityTabFragment
import org.wikipedia.activitytab.ActivityTabOnboardingActivity
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.databinding.FragmentMainBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ImportReadingListsEvent
import org.wikipedia.events.LoggedOutEvent
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.events.NewRecommendedReadingListEvent
import org.wikipedia.feed.HomeFragment
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.news.NewsActivity
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.MediaDownloadReceiver
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryFragment
import org.wikipedia.login.LoginActivity
import org.wikipedia.navtab.MenuNavTabDialog
import org.wikipedia.navtab.NavTab
import org.wikipedia.navtab.NavTabFragmentPagerAdapter
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.places.PlacesActivity
import org.wikipedia.random.RandomActivity
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.ReadingListsFragment
import org.wikipedia.readinglist.RemoveFromReadingListsDialog
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.search.SearchActivity
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.staticdata.MainPageNameData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.usercontrib.UserContribListActivity
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.TabUtil
import org.wikipedia.views.NotificationButtonView
import org.wikipedia.views.TabCountsView
import org.wikipedia.views.imageservice.ImageService
import org.wikipedia.watchlist.WatchlistActivity
import org.wikipedia.widgets.SearchWidgetInstallDialog
import org.wikipedia.yearinreview.YearInReviewDialog
import org.wikipedia.yearinreview.YearInReviewOnboardingActivity
import org.wikipedia.yearinreview.YearInReviewViewModel
import java.io.File
import java.util.concurrent.TimeUnit

class MainFragment : Fragment(), BackPressedHandler, MenuProvider, HistoryFragment.Callback, MenuNavTabDialog.Callback, ActivityTabFragment.Callback {
    interface Callback {
        fun onTabChanged(tab: NavTab)
        fun updateToolbarElevation(elevate: Boolean)
    }

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!

    private lateinit var notificationButtonView: NotificationButtonView
    private var tabCountsView: TabCountsView? = null
    private var showTabCountsAnimation = false
    private val downloadReceiver = MediaDownloadReceiver()
    private val downloadReceiverCallback = MediaDownloadReceiverCallback()
    private val pageChangeCallback = PageChangeCallback()

    // The permissions request API doesn't take a callback, so in the event we have to
    // ask for permission to download a featured image from the feed, we'll have to hold
    // the image we're waiting for permission to download as a bit of state here. :(
    private var pendingDownloadImage: FeaturedImage? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            pendingDownloadImage?.let { download(it) }
        } else {
            FeedbackUtil.showMessage(this, R.string.gallery_save_image_write_permission_rationale)
        }
    }

    private val activityTabOnboardingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onNavigateTo(NavTab.EDITS)
        }
    }

    var navTabBackStack = mutableListOf<NavTab>()
    val currentFragment get() = (binding.mainViewPager.adapter as NavTabFragmentPagerAdapter).getFragmentAt(binding.mainViewPager.currentItem)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        is LoggedOutEvent,
                        is LoggedOutInBackgroundEvent -> {
                            requireActivity().invalidateOptionsMenu()
                            (currentFragment as? HomeFragment)?.refreshNotification()
                            ExclusiveBottomSheetPresenter.dismiss(childFragmentManager)
                            refreshContents()
                        }
                        is ImportReadingListsEvent -> {
                            maybeShowImportReadingListsNewInstallDialog()
                        }
                        is NewRecommendedReadingListEvent -> {
                            binding.mainNavTabLayout.setOverlayDot(NavTab.READING_LISTS, Prefs.isRecommendedReadingListEnabled && Prefs.isNewRecommendedReadingListGenerated)
                        }
                    }
                }
            }
        }

        binding.mainViewPager.isUserInputEnabled = false
        binding.mainViewPager.adapter = NavTabFragmentPagerAdapter(this)
        binding.mainViewPager.registerOnPageChangeCallback(pageChangeCallback)
        binding.mainNavTabLayout.descendants.filterIsInstance<TextView>().forEach {
            it.maxLines = 2
        }
        val shouldShowRedDotForRecommendedReadingList = (!Prefs.isRecommendedReadingListOnboardingShown) || (Prefs.isRecommendedReadingListEnabled && Prefs.isNewRecommendedReadingListGenerated)
        binding.mainNavTabLayout.setOverlayDot(NavTab.READING_LISTS, shouldShowRedDotForRecommendedReadingList)
        binding.mainNavTabLayout.setOnItemSelectedListener { item ->
            navTabBackStack.clear()
            if (item.order == NavTab.EDITS.code()) {
                if (!Prefs.isActivityTabOnboardingShown) {
                    activityTabOnboardingLauncher.launch(ActivityTabOnboardingActivity.newIntent(requireContext()))
                    binding.mainNavTabLayout.setOverlayDot(NavTab.EDITS, false)
                    return@setOnItemSelectedListener false
                }
            }
            if (item.order == NavTab.MORE.code()) {
                ExclusiveBottomSheetPresenter.show(childFragmentManager, MenuNavTabDialog.newInstance())
                return@setOnItemSelectedListener false
            }
            val fragment = currentFragment
            if (fragment is HistoryFragment && item.order == NavTab.SEARCH.code()) {
                openSearchActivity(InvokeSource.NAV_MENU, null, null)
                return@setOnItemSelectedListener true
            }
            binding.mainViewPager.setCurrentItem(item.order, false)
            requireActivity().invalidateOptionsMenu()
            if (item.order == NavTab.SEARCH.code()) {
                maybeShowSearchWidgetInstallPrompt()
            }
            true
        }

        binding.mainNavTabLayout.setOverlayDot(NavTab.EDITS, !Prefs.isActivityTabOnboardingShown)

        maybeShowFeedNewModulesTooltip()
        Prefs.incrementExploreFeedVisitCount()

        notificationButtonView = NotificationButtonView(requireActivity())

        if (savedInstanceState == null) {
            handleIntent(requireActivity().intent)
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        downloadReceiver.unregister(requireContext())
    }

    override fun onResume() {
        super.onResume()
        downloadReceiver.register(requireContext(), downloadReceiverCallback)
        // reset the last-page-viewed timer
        Prefs.pageLastShown = 0
        YearInReviewDialog.maybeShowYearInReviewFeedbackDialog(requireActivity())
        if (YearInReviewViewModel.getYearInReviewModel()?.isReadingListCreated == true) {
            onNavigateTo(NavTab.READING_LISTS) // Navigate to reading lists only if Year in Review reading list is created
            YearInReviewViewModel.updateYearInReviewModel { it.copy(isReadingListCreated = false) }
        }
    }

    override fun onDestroyView() {
        Prefs.isSuggestedEditsHighestPriorityEnabled = false
        binding.mainViewPager.adapter = null
        binding.mainViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_VOICE_SEARCH && resultCode == Activity.RESULT_OK && data != null && data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            val searchQuery = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
            openSearchActivity(InvokeSource.VOICE, searchQuery, null)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY &&
                resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED && data != null) {
            startActivity(data)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN &&
                resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            refreshContents()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_BROWSE_TABS) {
            if (WikipediaApp.instance.tabCount == 0) {
                // They browsed the tabs and cleared all of them, without wanting to open a new tab.
                return
            }
            if (resultCode == TabActivity.RESULT_NEW_TAB) {
                val entry = HistoryEntry(PageTitle(
                    MainPageNameData.valueFor(WikipediaApp.instance.appOrSystemLanguageCode),
                        WikipediaApp.instance.wikiSite), HistoryEntry.SOURCE_MAIN_PAGE)
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
            } else if (resultCode == TabActivity.RESULT_LOAD_FROM_BACKSTACK) {
                startActivity(PageActivity.newIntent(requireContext()))
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY && resultCode == SearchFragment.RESULT_LANG_CHANGED ||
                (requestCode == Constants.ACTIVITY_REQUEST_SETTINGS &&
                        (resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED ||
                                resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED ||
                                resultCode == SettingsActivity.ACTIVITY_RESULT_LOG_OUT))) {
            refreshContents()
            if (resultCode == SettingsActivity.ACTIVITY_RESULT_LOG_OUT) {
                FeedbackUtil.showMessage(requireActivity(), R.string.toast_logout_complete)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val fragment = currentFragment
        return when (menuItem.itemId) {
            R.id.menu_search_lists -> {
                if (fragment is ReadingListsFragment) {
                    fragment.startSearchActionMode()
                }
                true
            }
            R.id.menu_overflow_button -> {
                if (fragment is ReadingListsFragment) {
                    fragment.showReadingListsOverflowMenu()
                }
                true
            }
            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_search_lists).isVisible = currentFragment is ReadingListsFragment
        menu.findItem(R.id.menu_overflow_button).isVisible = currentFragment is ReadingListsFragment

        val tabsItem = menu.findItem(R.id.menu_tabs)
        if (WikipediaApp.instance.tabCount < 1 || currentFragment is SuggestedEditsTasksFragment) {
            tabsItem.isVisible = false
            tabCountsView = null
        } else {
            tabsItem.isVisible = true
            tabCountsView = TabCountsView(requireActivity(), null)
            tabCountsView!!.setOnClickListener {
                if (WikipediaApp.instance.tabCount == 1) {
                    startActivity(PageActivity.newIntent(requireActivity()))
                } else {
                    startActivityForResult(TabActivity.newIntent(requireActivity()), Constants.ACTIVITY_REQUEST_BROWSE_TABS)
                }
            }
            tabCountsView!!.updateTabCount(showTabCountsAnimation)
            tabCountsView!!.contentDescription = getString(R.string.menu_page_show_tabs)
            tabsItem.actionView = tabCountsView
            tabsItem.expandActionView()
            FeedbackUtil.setButtonTooltip(tabCountsView!!)
            showTabCountsAnimation = false
        }
        val notificationMenuItem = menu.findItem(R.id.menu_notifications)
        if (AccountUtil.isLoggedIn) {
            notificationMenuItem.isVisible = true
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            notificationButtonView.setOnClickListener {
                if (AccountUtil.isLoggedIn) {
                    startActivity(NotificationActivity.newIntent(requireActivity()))
                }
            }
            notificationButtonView.contentDescription = getString(R.string.notifications_activity_title)
            notificationMenuItem.actionView = notificationButtonView
            notificationMenuItem.expandActionView()
            FeedbackUtil.setButtonTooltip(notificationButtonView)
        } else {
            notificationMenuItem.isVisible = false
        }
        updateNotificationDot(false)
    }

    fun handleIntent(intent: Intent) {
        if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_RANDOMIZER)) {
            startActivity(RandomActivity.newIntent(requireActivity(), WikipediaApp.instance.wikiSite, InvokeSource.APP_SHORTCUTS))
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_SEARCH)) {
            openSearchActivity(InvokeSource.APP_SHORTCUTS, null, null)
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            startActivity(PageActivity.newIntent(requireActivity()))
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_PLACES)) {
            startActivity(PlacesActivity.newIntent(requireActivity()))
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            onNavigateTo(NavTab.READING_LISTS)
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB) &&
                !(binding.mainNavTabLayout.selectedItemId == NavTab.HOME.code() &&
                        intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.HOME.code()) == NavTab.HOME.code())) {
            onNavigateTo(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.HOME.code())))
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB)) {
            onNavigateTo(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, NavTab.EDITS.code())))
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_PREVIEW_SAVED_READING_LISTS)) {
            onNavigateTo(NavTab.READING_LISTS)
        } else if (lastPageViewedWithin(1) && !intent.hasExtra(Constants.INTENT_RETURN_TO_MAIN) && WikipediaApp.instance.tabCount > 0) {
            startActivity(PageActivity.newIntent(requireContext()))
        }
    }

    fun onFeedVoiceSearchRequested() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        try {
            startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH)
        } catch (a: ActivityNotFoundException) {
            FeedbackUtil.showMessage(this, R.string.error_voice_search_not_available)
        }
    }

    fun onFeedSelectPage(entry: HistoryEntry, openInNewBackgroundTab: Boolean) {
        if (openInNewBackgroundTab) {
            TabUtil.openInNewBackgroundTab(entry)
            showTabCountsAnimation = true
            requireActivity().invalidateOptionsMenu()
        } else {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
        }
    }

    fun onFeedAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
        ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), entry.title, addToDefault, InvokeSource.FEED)
    }

    fun onFeedMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
        ReadingListBehaviorsUtil.moveToList(requireActivity(), sourceReadingListId, entry.title, InvokeSource.FEED)
    }

    fun onFeedRemovePageFromList(entry: HistoryEntry, lists: List<ReadingList>) {
        RemoveFromReadingListsDialog(lists).deleteOrShowDialog(requireActivity()) { readingLists, _ ->
            if (!requireActivity().isDestroyed) {
                val names = readingLists.map { it.title }.run {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ListFormatter.getInstance().format(this)
                    } else {
                        joinToString(separator = ", ")
                    }
                }
                FeedbackUtil.showMessage(requireActivity(), getString(R.string.reading_list_item_deleted_from_list, entry.title.displayText, names))
            }
        }
    }

    fun onFeedSharePage(entry: HistoryEntry) {
        ShareUtil.shareText(requireContext(), entry.title.displayText, entry.title.uri)
    }

    fun onFeedCopyLink(entry: HistoryEntry) {
        ClipboardUtil.setPlainText(requireContext(), text = entry.title.uri)
        FeedbackUtil.showMessage(requireActivity(), R.string.address_copied)
    }

    fun onFeedNewsItemSelected(newsItem: NewsItem, wikiSite: WikiSite) {
        startActivity(NewsActivity.newIntent(requireActivity(), newsItem, wikiSite))
    }

    fun onFeedShareImage(image: FeaturedImage, age: Int) {
        val thumbUrl = image.thumbnailUrl
        val fullSizeUrl = image.original.source
        ImageService.loadImage(requireContext(), thumbUrl, onSuccess = { bitmap ->
            if (!isAdded) {
                return@loadImage
            }
            ShareUtil.shareImage(lifecycleScope, requireContext(), bitmap, File(thumbUrl).name,
                ShareUtil.getFeaturedImageShareSubject(requireContext(), age), fullSizeUrl)
        })
    }

    fun onFeedDownloadImage(image: FeaturedImage) {
        pendingDownloadImage = image
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            download(image)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    fun onFeaturedImageSelected(image: FeaturedImage) {
        startActivity(FilePageActivity.newIntent(requireActivity(), image.toPageTitle()))
    }

    fun onLoginRequested() {
        startActivityForResult(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_NAV),
                Constants.ACTIVITY_REQUEST_LOGIN)
    }

    override fun onLoadPage(entry: HistoryEntry) {
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
    }

    override fun onBackPressed(): Boolean {
        if ((currentFragment as? BackPressedHandler)?.onBackPressed() == true) {
            return true
        } else if (navTabBackStack.isNotEmpty()) {
            onNavigateTo(navTabBackStack.removeLastOrNull()!!)
            return true
        }
        return false
    }

    override fun usernameClick() {
        val pageTitle = PageTitle(UserAliasData.valueFor(WikipediaApp.instance.languageState.appLanguageCode), AccountUtil.userName, WikipediaApp.instance.wikiSite)
        val entry = HistoryEntry(pageTitle, HistoryEntry.SOURCE_MAIN_PAGE)
        startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, pageTitle))
    }

    override fun loginClick() {
        onLoginRequested()
    }

    override fun talkClick() {
        if (AccountUtil.isLoggedIn) {
            startActivity(TalkTopicsActivity.newIntent(requireActivity(),
                PageTitle(UserTalkAliasData.valueFor(WikipediaApp.instance.languageState.appLanguageCode), AccountUtil.userName,
                    WikiSite.forLanguageCode(WikipediaApp.instance.appOrSystemLanguageCode)), InvokeSource.NAV_MENU))
        }
    }

    override fun settingsClick() {
        startActivityForResult(SettingsActivity.newIntent(requireActivity()), Constants.ACTIVITY_REQUEST_SETTINGS)
    }

    override fun watchlistClick() {
        if (AccountUtil.isLoggedIn) {
            startActivity(WatchlistActivity.newIntent(requireActivity()))
        }
    }

    override fun contribsClick() {
        if (AccountUtil.isLoggedIn) {
            startActivity(UserContribListActivity.newIntent(requireActivity(), AccountUtil.userName))
        }
    }

    override fun donateClick(campaignId: String?) {
        (requireActivity() as? BaseActivity)?.launchDonateDialog(campaignId = campaignId)
    }

    override fun yearInReviewClick() {
        startActivity(YearInReviewOnboardingActivity.newIntent(requireActivity()))
    }

    fun setBottomNavVisible(visible: Boolean) {
        binding.mainNavTabBorder.isVisible = visible
        binding.mainNavTabLayout.isVisible = visible
    }

    fun onGoOffline() {
        val fragment = currentFragment
        if (fragment is HistoryFragment) {
            fragment.refresh()
        }
    }

    fun onGoOnline() {
        val fragment = currentFragment
        if (fragment is HistoryFragment) {
            fragment.refresh()
        }
    }

    fun updateNotificationDot(animate: Boolean) {
        if (AccountUtil.isLoggedIn && Prefs.notificationUnreadCount > 0) {
            notificationButtonView.setUnreadCount(Prefs.notificationUnreadCount)
            if (animate) {
                notificationButtonView.runAnimation()
            }
        } else {
            notificationButtonView.setUnreadCount(0)
        }
    }

    @Suppress("SameParameterValue")
    private fun lastPageViewedWithin(days: Int): Boolean {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.pageLastShown) < days
    }

    private fun download(image: FeaturedImage) {
        pendingDownloadImage = null
        downloadReceiver.download(requireContext(), image)
        FeedbackUtil.showMessage(this, R.string.gallery_save_progress)
    }

    fun openSearchActivity(source: InvokeSource, query: String?, transitionView: View?) {
        val intent = SearchActivity.newIntent(requireActivity(), source, query)
        val options = transitionView?.let {
            if (intent.component?.className == SearchActivity::class.java.name) {
                ActivityOptions.makeSceneTransitionAnimation(requireActivity(), it, getString(R.string.transition_search_bar))
            } else null
        }
        startActivityForResult(intent, Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY, options?.toBundle())
    }

    private fun refreshContents() {
        when (val fragment = currentFragment) {
            is ReadingListsFragment -> fragment.updateLists()
            is HistoryFragment -> fragment.refresh()
            is SuggestedEditsTasksFragment -> fragment.refreshContents()
        }
    }

    private fun maybeShowImportReadingListsNewInstallDialog() {
        if (!Prefs.importReadingListsNewInstallDialogShown) {
            ReadingListsAnalyticsHelper.logReceiveStart(requireActivity())
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.shareable_reading_lists_new_install_dialog_title)
                .setMessage(R.string.shareable_reading_lists_new_install_dialog_content)
                .setNegativeButton(R.string.shareable_reading_lists_new_install_dialog_got_it, null)
                .show()
            Prefs.importReadingListsNewInstallDialogShown = true
        }
    }

    private fun maybeShowFeedNewModulesTooltip() {
        if (Prefs.exploreFeedVisitCount == 0) {
            // Explicitly consider this tooltip "shown", since we only want to show it to users
            // who have used the Feed already, instead of completely new users.
            Prefs.isHomeFeedUpdateTooltipShown = true
        } else if (!Prefs.isHomeFeedUpdateTooltipShown) {
            Prefs.isHomeFeedUpdateTooltipShown = true
            binding.root.post {
                if (isAdded) {
                    FeedbackUtil.showTooltip(requireActivity(), binding.mainNavTabLayout.findViewById(NavTab.HOME.id), getString(R.string.home_feed_update_tooltip1), aboveOrBelow = true, autoDismiss = false, showDismissButton = true)
                }
            }
        }
    }

    private fun maybeShowSearchWidgetInstallPrompt() {
        if (!Prefs.searchWidgetInstallPromptShown && !SearchWidgetInstallDialog.isWidgetInstalled()) {
            ExclusiveBottomSheetPresenter.show(childFragmentManager, SearchWidgetInstallDialog())
        }
    }

    private inner class PageChangeCallback : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            callback()?.onTabChanged(NavTab.of(position))
        }
    }

    private inner class MediaDownloadReceiverCallback : MediaDownloadReceiver.Callback {
        override fun onSuccess() {
            FeedbackUtil.showMessage(requireActivity(), R.string.gallery_save_success)
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    override fun onNavigateTo(navTab: NavTab) {
        val lastNavTab = NavTab.entries.find { binding.mainNavTabLayout.selectedItemId == binding.mainNavTabLayout.menu[it.code()].itemId }
        binding.mainNavTabLayout.selectedItemId = binding.mainNavTabLayout.menu[navTab.code()].itemId
        if (lastNavTab == NavTab.EDITS && navTab != NavTab.EDITS) {
            navTabBackStack.add(NavTab.EDITS)
        }
    }

    companion object {
        fun newInstance(): MainFragment {
            return MainFragment().apply {
                retainInstance = true
            }
        }
    }
}
