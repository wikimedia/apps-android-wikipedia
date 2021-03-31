package org.wikipedia.main

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.wikipedia.BackPressedHandler
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.analytics.WatchlistFunnel
import org.wikipedia.auth.AccountUtil.isLoggedIn
import org.wikipedia.auth.AccountUtil.userName
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.FragmentMainBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.feed.FeedFragment
import org.wikipedia.feed.image.FeaturedImage
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.news.NewsActivity
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItemView
import org.wikipedia.gallery.GalleryActivity
import org.wikipedia.gallery.ImagePipelineBitmapGetter
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
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.random.RandomActivity
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.ReadingListsFragment
import org.wikipedia.search.SearchActivity
import org.wikipedia.search.SearchFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity
import org.wikipedia.settings.SiteInfoClient.getMainPageForLang
import org.wikipedia.staticdata.UserTalkAliasData
import org.wikipedia.suggestededits.ImageRecsFragment
import org.wikipedia.suggestededits.SuggestedEditsTasksFragment
import org.wikipedia.talk.TalkTopicsActivity
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.watchlist.WatchlistActivity
import java.io.File
import java.util.concurrent.TimeUnit

class MainFragment : Fragment(), BackPressedHandler, FeedFragment.Callback, HistoryFragment.Callback, LinkPreviewDialog.Callback, MenuNavTabDialog.Callback {
    interface Callback {
        fun onTabChanged(tab: NavTab)
        fun updateTabCountsView()
        fun updateToolbarElevation(elevate: Boolean)
    }

    private var _binding: FragmentMainBinding? = null
    val binding get() = _binding!!

    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val downloadReceiver = MediaDownloadReceiver()
    private val downloadReceiverCallback = MediaDownloadReceiverCallback()
    private val pageChangeCallback = PageChangeCallback()
    private val disposables = CompositeDisposable()
    private var exclusiveTooltipRunnable: Runnable? = null

    // The permissions request API doesn't take a callback, so in the event we have to
    // ask for permission to download a featured image from the feed, we'll have to hold
    // the image we're waiting for permission to download as a bit of state here. :(
    private var pendingDownloadImage: FeaturedImage? = null

    val currentFragment get() = (binding.mainViewPager.adapter as NavTabFragmentPagerAdapter).getFragmentAt(binding.mainViewPager.currentItem)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentMainBinding.inflate(inflater, container, false)

        disposables.add(WikipediaApp.getInstance().bus.subscribe(EventBusConsumer()))
        binding.mainViewPager.isUserInputEnabled = false
        binding.mainViewPager.adapter = NavTabFragmentPagerAdapter(this)
        binding.mainViewPager.registerOnPageChangeCallback(pageChangeCallback)

        FeedbackUtil.setButtonLongPressToast(binding.navMoreContainer)
        binding.navMoreContainer.setOnClickListener {
            bottomSheetPresenter.show(childFragmentManager, MenuNavTabDialog.newInstance())
        }

        binding.mainNavTabLayout.setOnNavigationItemSelectedListener { item ->
            if (currentFragment is FeedFragment && item.order == 0) {
                (currentFragment as FeedFragment?)!!.scrollToTop()
            }
            if (currentFragment is HistoryFragment && item.order == NavTab.SEARCH.code()) {
                openSearchActivity(InvokeSource.NAV_MENU, null, null)
                return@setOnNavigationItemSelectedListener true
            }
            binding.mainViewPager.setCurrentItem(item.order, false)
            true
        }

        maybeShowEditsTooltip()
        maybeShowImageRecsTooltip()

        if (savedInstanceState == null) {
            handleIntent(requireActivity().intent)
        }
        return binding.root
    }

    override fun onPause() {
        super.onPause()
        downloadReceiver.callback = null
        requireContext().unregisterReceiver(downloadReceiver)
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        downloadReceiver.callback = downloadReceiverCallback
        // reset the last-page-viewed timer
        Prefs.pageLastShown(0)
        maybeShowWatchlistTooltip()
    }

    override fun onDestroyView() {
        Prefs.setSuggestedEditsHighestPriorityEnabled(false)
        binding.mainViewPager.adapter = null
        binding.mainViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
        disposables.dispose()
        super.onDestroyView()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.ACTIVITY_REQUEST_VOICE_SEARCH && resultCode == Activity.RESULT_OK && data != null && data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) != null) {
            val searchQuery = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)!![0]
            openSearchActivity(InvokeSource.VOICE, searchQuery, null)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY &&
                resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED) {
            startActivity(data)
        } else if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN &&
                resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            refreshContents()
            if (!Prefs.shouldShowSuggestedEditsTooltip()) {
                FeedbackUtil.showMessage(this, R.string.login_success_toast)
            }
            maybeShowImageRecsTooltip()
        } else if (requestCode == Constants.ACTIVITY_REQUEST_BROWSE_TABS) {
            if (WikipediaApp.getInstance().tabCount == 0) {
                // They browsed the tabs and cleared all of them, without wanting to open a new tab.
                return
            }
            if (resultCode == TabActivity.RESULT_NEW_TAB) {
                val entry = HistoryEntry(PageTitle(getMainPageForLang(WikipediaApp.getInstance().appOrSystemLanguageCode),
                        WikipediaApp.getInstance().wikiSite), HistoryEntry.SOURCE_MAIN_PAGE)
                startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
            } else if (resultCode == TabActivity.RESULT_LOAD_FROM_BACKSTACK) {
                startActivity(PageActivity.newIntent(requireContext()))
            }
        } else if (requestCode == Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY && resultCode == SearchFragment.RESULT_LANG_CHANGED ||
                (requestCode == Constants.ACTIVITY_REQUEST_SETTINGS &&
                        (resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED || resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED))) {
            refreshContents()
            if (resultCode == SettingsActivity.ACTIVITY_RESULT_FEED_CONFIGURATION_CHANGED) {
                updateFeedHiddenCards()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION -> if (PermissionUtil.isPermitted(grantResults)) {
                pendingDownloadImage?.let {
                    download(it)
                }
            } else {
                setPendingDownload(null)
                L.d("Write permission was denied by user")
                FeedbackUtil.showMessage(this, R.string.gallery_save_image_write_permission_rationale)
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun handleIntent(intent: Intent) {
        if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_RANDOMIZER)) {
            startActivity(RandomActivity.newIntent(requireActivity(), WikipediaApp.getInstance().wikiSite, InvokeSource.APP_SHORTCUTS))
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_SEARCH)) {
            openSearchActivity(InvokeSource.APP_SHORTCUTS, null, null)
        } else if (intent.hasExtra(Constants.INTENT_APP_SHORTCUT_CONTINUE_READING)) {
            startActivity(PageActivity.newIntent(requireActivity()))
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_DELETE_READING_LIST)) {
            goToTab(NavTab.READING_LISTS)
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB) &&
                !(binding.mainNavTabLayout.selectedItemId == NavTab.EXPLORE.code() &&
                        intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code()) == NavTab.EXPLORE.code())) {
            goToTab(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_MAIN_TAB, NavTab.EXPLORE.code())))
        } else if (intent.hasExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB)) {
            goToTab(NavTab.of(intent.getIntExtra(Constants.INTENT_EXTRA_GO_TO_SE_TAB, NavTab.EDITS.code())))
        } else if (lastPageViewedWithin(1) && !intent.hasExtra(Constants.INTENT_RETURN_TO_MAIN) && WikipediaApp.getInstance().tabCount > 0) {
            startActivity(PageActivity.newIntent(requireContext()))
        }
    }

    override fun onFeedSearchRequested(view: View) {
        openSearchActivity(InvokeSource.FEED_BAR, null, view)
    }

    override fun onFeedVoiceSearchRequested() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        try {
            startActivityForResult(intent, Constants.ACTIVITY_REQUEST_VOICE_SEARCH)
        } catch (a: ActivityNotFoundException) {
            FeedbackUtil.showMessage(this, R.string.error_voice_search_not_available)
        }
    }

    override fun onFeedSelectPage(entry: HistoryEntry, openInNewBackgroundTab: Boolean) {
        if (openInNewBackgroundTab) {
            TabUtil.openInNewBackgroundTab(entry)
            callback()?.updateTabCountsView()
        } else {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
        }
    }

    override fun onFeedSelectPageWithAnimation(entry: HistoryEntry, sharedElements: Array<Pair<View, String>>) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), *sharedElements)
        val intent = PageActivity.newIntentForNewTab(requireContext(), entry, entry.title)
        if (sharedElements.isNotEmpty()) {
            intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true)
        }
        startActivity(intent, if (DimenUtil.isLandscape(requireContext()) || sharedElements.isEmpty()) null else options.toBundle())
    }

    override fun onFeedAddPageToList(entry: HistoryEntry, addToDefault: Boolean) {
        if (addToDefault) {
            ReadingListBehaviorsUtil.addToDefaultList(requireActivity(), entry.title, InvokeSource.FEED) { readingListId -> onFeedMovePageToList(readingListId, entry) }
        } else {
            bottomSheetPresenter.show(childFragmentManager, AddToReadingListDialog.newInstance(entry.title, InvokeSource.FEED))
        }
    }

    override fun onFeedMovePageToList(sourceReadingListId: Long, entry: HistoryEntry) {
        bottomSheetPresenter.show(childFragmentManager,
                MoveToReadingListDialog.newInstance(sourceReadingListId, entry.title, InvokeSource.FEED))
    }

    override fun onFeedNewsItemSelected(newsCard: NewsCard, view: NewsItemView) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view.imageView, getString(R.string.transition_news_item))
        view.newsItem?.let {
            startActivity(NewsActivity.newIntent(requireActivity(), it, newsCard.wikiSite()), if (it.thumb() != null) options.toBundle() else null)
        }
    }

    override fun onFeedSeCardFooterClicked() {
        goToTab(NavTab.EDITS)
    }

    override fun onFeedShareImage(card: FeaturedImageCard) {
        val thumbUrl = card.baseImage().thumbnailUrl
        val fullSizeUrl = card.baseImage().original.source
        object : ImagePipelineBitmapGetter(thumbUrl) {
            override fun onSuccess(bitmap: Bitmap?) {
                if (bitmap != null) {
                    ShareUtil.shareImage(requireContext(), bitmap, File(thumbUrl).name,
                            ShareUtil.getFeaturedImageShareSubject(requireContext(), card.age()), fullSizeUrl)
                } else {
                    FeedbackUtil.showMessage(this@MainFragment, getString(R.string.gallery_share_error, card.baseImage().title()))
                }
            }
        }[requireContext()]
    }

    override fun onFeedDownloadImage(image: FeaturedImage) {
        if (!PermissionUtil.hasWriteExternalStoragePermission(requireContext())) {
            setPendingDownload(image)
            requestWriteExternalStoragePermission()
        } else {
            download(image)
        }
    }

    override fun onFeaturedImageSelected(card: FeaturedImageCard) {
        startActivity(FilePageActivity.newIntent(requireActivity(), PageTitle(card.filename(), card.wikiSite())))
    }

    override fun onLoginRequested() {
        startActivityForResult(LoginActivity.newIntent(requireContext(), LoginFunnel.SOURCE_NAV),
                Constants.ACTIVITY_REQUEST_LOGIN)
    }

    override fun updateToolbarElevation(elevate: Boolean) {
        callback()?.updateToolbarElevation(elevate)
    }

    fun requestUpdateToolbarElevation() {
        val fragment = currentFragment
        updateToolbarElevation(fragment is FeedFragment && fragment.shouldElevateToolbar())
    }

    override fun onLoadPage(entry: HistoryEntry) {
        startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        if (inNewTab) {
            startActivity(PageActivity.newIntentForNewTab(requireContext(), entry, entry.title))
        } else {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.title))
        }
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        copyLink(title.uri)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        bottomSheetPresenter.show(childFragmentManager, AddToReadingListDialog.newInstance(title, InvokeSource.LINK_PREVIEW_MENU))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(requireContext(), title)
    }

    override fun onBackPressed(): Boolean {
        val fragment = currentFragment
        return fragment is BackPressedHandler && (fragment as BackPressedHandler).onBackPressed()
    }

    override fun loginLogoutClick() {
        if (isLoggedIn) {
            AlertDialog.Builder(requireContext())
                    .setMessage(R.string.logout_prompt)
                    .setNegativeButton(R.string.logout_dialog_cancel_button_text, null)
                    .setPositiveButton(R.string.preference_title_logout) { _, _ ->
                        WikipediaApp.getInstance().logOut()
                        FeedbackUtil.showMessage(requireActivity(), R.string.toast_logout_complete)
                        Prefs.setReadingListsLastSyncTime(null)
                        Prefs.setReadingListSyncEnabled(false)
                        Prefs.setSuggestedEditsHighestPriorityEnabled(false)
                        refreshContents()
                    }.show()
        } else {
            onLoginRequested()
        }
    }

    override fun notificationsClick() {
        if (isLoggedIn) {
            startActivity(NotificationActivity.newIntent(requireActivity()))
        }
    }

    override fun talkClick() {
        if (isLoggedIn) {
            userName?.let {
                startActivity(TalkTopicsActivity.newIntent(requireActivity(),
                        PageTitle(UserTalkAliasData.valueFor(WikipediaApp.getInstance().language().appLanguageCode), it,
                                WikiSite.forLanguageCode(WikipediaApp.getInstance().appOrSystemLanguageCode)), InvokeSource.NAV_MENU))
            }
        }
    }

    override fun historyClick() {
        if (currentFragment !is HistoryFragment) {
            goToTab(NavTab.SEARCH)
        }
    }

    override fun settingsClick() {
        startActivityForResult(SettingsActivity.newIntent(requireActivity()), Constants.ACTIVITY_REQUEST_SETTINGS)
    }

    override fun watchlistClick() {
        if (isLoggedIn) {
            WatchlistFunnel().logViewWatchlist()
            startActivity(WatchlistActivity.newIntent(requireActivity()))
        }
    }

    fun setBottomNavVisible(visible: Boolean) {
        binding.mainNavTabContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun onGoOffline() {
        val fragment = currentFragment
        if (fragment is FeedFragment) {
            fragment.onGoOffline()
        } else if (fragment is HistoryFragment) {
            fragment.refresh()
        }
    }

    fun onGoOnline() {
        val fragment = currentFragment
        if (fragment is FeedFragment) {
            fragment.onGoOnline()
        } else if (fragment is HistoryFragment) {
            fragment.refresh()
        }
    }

    private fun copyLink(url: String) {
        ClipboardUtil.setPlainText(requireContext(), null, url)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    @Suppress("SameParameterValue")
    private fun lastPageViewedWithin(days: Int): Boolean {
        return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - Prefs.pageLastShown()) < days
    }

    private fun download(image: FeaturedImage) {
        setPendingDownload(null)
        downloadReceiver.download(requireContext(), image)
        FeedbackUtil.showMessage(this, R.string.gallery_save_progress)
    }

    private fun setPendingDownload(image: FeaturedImage?) {
        pendingDownloadImage = image
    }

    private fun requestWriteExternalStoragePermission() {
        PermissionUtil.requestWriteStorageRuntimePermissions(this,
                Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION)
    }

    fun openSearchActivity(source: InvokeSource, query: String?, transitionView: View?) {
        val intent = SearchActivity.newIntent(requireActivity(), source, query)
        var options: ActivityOptionsCompat? = null
        transitionView?.let {
            options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), it, getString(R.string.transition_search_bar))
        }
        startActivityForResult(intent, Constants.ACTIVITY_REQUEST_OPEN_SEARCH_ACTIVITY, options?.toBundle())
    }

    private fun goToTab(tab: NavTab) {
        binding.mainNavTabLayout.selectedItemId = binding.mainNavTabLayout.menu.getItem(tab.code()).itemId
    }

    private fun refreshContents() {
        when (val fragment = currentFragment) {
            is FeedFragment -> fragment.refresh()
            is ReadingListsFragment -> fragment.updateLists()
            is HistoryFragment -> fragment.refresh()
            is SuggestedEditsTasksFragment -> fragment.refreshContents()
        }
    }

    private fun updateFeedHiddenCards() {
        val fragment = currentFragment
        if (fragment is FeedFragment) {
            fragment.updateHiddenCards()
        }
    }

    private fun maybeShowEditsTooltip() {
        if (currentFragment !is SuggestedEditsTasksFragment && Prefs.shouldShowSuggestedEditsTooltip() &&
                Prefs.getExploreFeedVisitCount() >= SHOW_EDITS_SNACKBAR_COUNT) {
            enqueueTooltip {
                FeedbackUtil.showTooltip(requireActivity(), binding.mainNavTabLayout.findViewById(NavTab.EDITS.id()), if (isLoggedIn) getString(R.string.main_tooltip_text, userName) else getString(R.string.main_tooltip_text_v2), aboveOrBelow = true, autoDismiss = false)
                        .setOnBalloonDismissListener {
                            Prefs.setShouldShowSuggestedEditsTooltip(false)
                        }
            }
        }
    }

    private fun maybeShowWatchlistTooltip() {
        if (Prefs.isWatchlistPageOnboardingTooltipShown() &&
                !Prefs.isWatchlistMainOnboardingTooltipShown() && isLoggedIn) {
            enqueueTooltip {
                FeedbackUtil.showTooltip(requireActivity(), binding.navMoreContainer, R.layout.view_watchlist_main_tooltip, 0, 0, aboveOrBelow = true, autoDismiss = false)
                        .setOnBalloonDismissListener {
                            WatchlistFunnel().logShowTooltipMore()
                            Prefs.setWatchlistMainOnboardingTooltipShown(true)
                        }
            }
        }
    }

    private fun maybeShowImageRecsTooltip() {
        if (ImageRecsFragment.isFeatureEnabled() && Prefs.shouldShowImageRecsTooltip() &&
                Prefs.getExploreFeedVisitCount() >= SHOW_EDITS_SNACKBAR_COUNT) {
            enqueueTooltip {
                FeedbackUtil.showTooltip(requireActivity(), binding.mainNavTabLayout.findViewById(NavTab.EDITS.id()), R.layout.view_image_recs_tooltip, 0, 0, aboveOrBelow = true, autoDismiss = false)
                        .setOnBalloonDismissListener {
                            Prefs.setShowImageRecsTooltip(false)
                        }
            }
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

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
            if (event is LoggedOutInBackgroundEvent) {
                refreshContents()
            }
        }
    }

    private fun enqueueTooltip(runnable: Runnable) {
        if (exclusiveTooltipRunnable != null) {
            return
        }
        exclusiveTooltipRunnable = runnable
        binding.mainNavTabLayout.postDelayed(500) {
            exclusiveTooltipRunnable = null
            if (!isAdded) {
                return@postDelayed
            }
            runnable.run()
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    companion object {
        // Actually shows on the 3rd time of using the app. The Pref.incrementExploreFeedVisitCount() gets call after MainFragment.onResume()
        private const val SHOW_EDITS_SNACKBAR_COUNT = 2

        fun newInstance(): MainFragment {
            return MainFragment().apply {
                retainInstance = true
            }
        }
    }
}
