package org.wikipedia.feed

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.NotificationBell
import org.wikipedia.compose.components.NotificationBellState
import org.wikipedia.compose.components.TabsBox
import org.wikipedia.compose.components.WikiLangCodeBox
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.components.menu.PageOverflowMenu
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.extensions.pulse
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.becauseyouread.BecauseYouReadModule
import org.wikipedia.feed.continuereading.ContinueReadingModule
import org.wikipedia.feed.dayheader.DayHeaderCard
import org.wikipedia.feed.featured.FeaturedArticleCard
import org.wikipedia.feed.featured.FeaturedArticleModule
import org.wikipedia.feed.image.FeaturedImageCard
import org.wikipedia.feed.image.FeaturedImageModule
import org.wikipedia.feed.interests.BasedOnInterestModule
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.news.NewsCard
import org.wikipedia.feed.news.NewsItem
import org.wikipedia.feed.news.NewsModule
import org.wikipedia.feed.onboarding.ExploreFeedUpdatePromptActivity
import org.wikipedia.feed.onthisday.OnThisDayActivity
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.onthisday.OnThisDayModule
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.feed.topread.TopReadListCard
import org.wikipedia.feed.topread.TopReadModule
import org.wikipedia.history.HistoryEntry
import org.wikipedia.language.AppLanguageState
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.homefeed.HomeFeedSettingsActivity
import org.wikipedia.settings.homefeed.HomeFeedSettingsStartDestination
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private val pageOverflowMenuViewModel: PageOverflowMenuViewModel by viewModels()
    private val cardImpressions = mutableSetOf<String>()
    private val instrument = TestKitchenAdapter.client.getInstrument("apps-home-feed")

    private val personalizationResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val tab = if (Prefs.homePreferenceSelection == HomePreferenceType.PERSONALIZED) HomeTab.FOR_YOU else HomeTab.COMMUNITY
            selectTab(tab)
        }
    }

    private val customizeInterestsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            selectTab(HomeTab.FOR_YOU)
            Prefs.homeForYouModulesToday = ""
            viewModel.refreshForYouContent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            maybeShowExploreFeedUpdatePrompt()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                val selectedTab by viewModel.selectedTab.collectAsState()
                val wikiSite by viewModel.wikiSite.collectAsState()
                val tabsState by viewModel.tabsState.collectAsState()
                val notificationState by viewModel.unreadCount.collectAsState()
                var swipeToExplorePromptShown by remember { mutableStateOf(Prefs.isHomeSwipeToExplorePromptShown) }

                BaseTheme(currentTheme = if (selectedTab == HomeTab.FOR_YOU) Theme.BLACK else WikipediaApp.instance.currentTheme) {
                    HomeScreen(
                        wikiSite = wikiSite,
                        languageState = WikipediaApp.instance.languageState,
                        selectedTab = selectedTab,
                        communityContentState = viewModel.communityState.collectAsState().value,
                        forYouContentState = viewModel.forYouState.collectAsState().value,
                        overflowMenuState = pageOverflowMenuViewModel.pageOverflowMenuState,
                        tabsState = tabsState,
                        notificationBellState = notificationState,
                        onSelectTab = {
                            selectTab(it)
                        },
                        onRefreshTab = {
                            if (it == HomeTab.COMMUNITY) {
                                viewModel.refreshCommunityContent()
                            } else {
                                viewModel.refreshForYouContent()
                            }
                        },
                        onLoadMoreCommunityContent = viewModel::loadCommunityContent,
                        onLoadMoreForYouContent = viewModel::loadForYouContent,
                        onHideCommunityCardClick = { card ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "card_hide")
                            val cardIndex = viewModel.hideCommunityCard(card)
                            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed))
                                .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "undo_card_hide")
                                    viewModel.restoreCommunityCard(card, cardIndex)
                                }.show()
                        },
                        onHideForYouCardClick = { module, card ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "card_hide")
                            val cardIndex = viewModel.hideForYouCard(module, card)
                            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed))
                                .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "undo_card_hide")
                                    viewModel.restoreForYouCard(module, card, cardIndex)
                                }.show()
                        },
                        onHideModuleClick = { moduleKey ->
                            instrument.submitInteraction("click", actionSource = moduleKey, actionSubtype = "feed_overflow", elementId = "module_hide")
                            viewModel.hideModule(moduleKey)
                            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.explore_feed_header_overflow_hide_module_message))
                                .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                                    instrument.submitInteraction("click", actionSource = moduleKey, actionSubtype = "feed_overflow", elementId = "undo_module_hide")
                                    viewModel.restoreModule(moduleKey)
                                }.show()
                        },
                        onPageClick = { card, historyEntry ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = historyEntry.title))
                            (parentFragment as? MainFragment)?.onFeedSelectPage(historyEntry, false)
                        },
                        onPageBookmarkClick = { card, historyEntry ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "article_save", pageData = TestKitchenAdapter.getPageData(pageTitle = historyEntry.title))
                            (parentFragment as? MainFragment)?.onFeedAddPageToList(historyEntry, true)
                        },
                        onPageShareClick = { card, historyEntry ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "article_share", pageData = TestKitchenAdapter.getPageData(pageTitle = historyEntry.title))
                            ShareUtil.shareText(requireContext(), historyEntry.title)
                        },
                        onPageOverflowClick = { card, pageSummary, source, menuKey ->
                            pageOverflowMenuViewModel.onPageOverflowClick(
                                context = requireContext(),
                                wikiSite = wikiSite,
                                pageSummary = pageSummary,
                                source = source,
                                menuKey = menuKey,
                                onOpenPage = { entry ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedSelectPage(entry, false)
                                },
                                onOpenInNewTab = { entry ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_open_new_tab", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedSelectPage(entry, true)
                                    viewModel.updateTabCount(true)
                                },
                                onAddRequest = { entry, addToDefault ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_save", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedAddPageToList(entry, addToDefault)
                                },
                                onMoveRequest = { id, entry ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_move", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedMovePageToList(id, entry)
                                },
                                onRemoveRequest = { entry, lists ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_remove", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedRemovePageFromList(entry, lists)
                                },
                                onShareRequest = { entry ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_share", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedSharePage(entry)
                                },
                                onLinkCopyRequest = { entry ->
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_item_overflow", elementId = "article_copy_link", pageData = TestKitchenAdapter.getPageData(pageTitle = entry.title))
                                    (parentFragment as? MainFragment)?.onFeedCopyLink(entry)
                                }
                            )
                        },
                        onPageOverflowDismiss = {
                            pageOverflowMenuViewModel.dismissPageOverflowMenu()
                        },
                        onNewsClick = { card, newsItem ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = card.javaClass.simpleName, actionContext = mapOf("index" to card.news.indexOf(newsItem)))
                            (parentFragment as? MainFragment)?.onFeedNewsItemSelected(newsItem, wikiSite)
                        },
                        onImageClick = {
                            instrument.submitInteraction("click", actionSource = it.javaClass.simpleName, elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = it.featuredImage.toPageTitle()))
                            (parentFragment as? MainFragment)?.onFeaturedImageSelected(it.featuredImage)
                        },
                        onImageShareClick = {
                            instrument.submitInteraction("click", actionSource = it.javaClass.simpleName, elementId = "share", pageData = TestKitchenAdapter.getPageData(pageTitle = it.featuredImage.toPageTitle()))
                            (parentFragment as? MainFragment)?.onFeedShareImage(it.featuredImage, it.age)
                        },
                        onImageDownloadClick = {
                            instrument.submitInteraction("click", actionSource = it.javaClass.simpleName, elementId = "download", pageData = TestKitchenAdapter.getPageData(pageTitle = it.featuredImage.toPageTitle()))
                            (parentFragment as? MainFragment)?.onFeedDownloadImage(it.featuredImage)
                        },
                        onLanguageSelected = { languageCode ->
                            instrument.submitInteraction("click", "language_menu", elementId = "language_change", actionContext = mapOf("selected_tab" to selectedTab.name, "language_code" to languageCode))
                            viewModel.updateLanguage(languageCode)
                        },
                        onManageLanguagesClick = {
                            instrument.submitInteraction("click", "language_menu", elementId = "manage_languages", actionContext = mapOf("selected_tab" to selectedTab.name))
                            requireActivity().startActivity(WikipediaLanguagesActivity.newIntent(requireContext(), invokeSource = InvokeSource.FEED))
                        },
                        onCustomizeInterestsClick = { card ->
                            instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "feed_customize")
                            customizeInterestsLauncher.launch(PersonalizationActivity.newIntent(requireContext(), showInterestsOnly = true))
                        },
                        onTabClick = {
                            requireActivity().startActivity(TabActivity.newIntent(requireActivity()))
                        },
                        onUpdateTabCount = {
                            viewModel.updateTabCount(false)
                        },
                        onCardImpression = { card, index ->
                            onCardImpression(card, index)
                        },
                        onCardFooterClick = { card ->
                            when (card) {
                                is TopReadListCard -> {
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "more_top_read")
                                    // TODO: simplify TopReadListCard after we remove the old feed UIs.
                                    startActivity(TopReadArticlesActivity.newIntent(requireActivity(), TopReadListCard(card.articles, card.age, wikiSite)))
                                }
                                is OnThisDayCard -> {
                                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "more_on_this_day")
                                    startActivity(OnThisDayActivity.newIntent(requireActivity(), card.age, -1, wikiSite, InvokeSource.ON_THIS_DAY_CARD_FOOTER))
                                }
                            }
                        },
                        onNotificationClick = {
                            requireActivity().startActivity(NotificationActivity.newIntent(requireActivity()))
                        },
                        onManageModulesClick = {
                            val intent = HomeFeedSettingsActivity.newIntent(
                                context = requireContext(),
                                startDestination = when (selectedTab) {
                                    HomeTab.COMMUNITY -> HomeFeedSettingsStartDestination.COMMUNITY_MODULES
                                    HomeTab.FOR_YOU -> HomeFeedSettingsStartDestination.FOR_YOU_MODULES
                                }
                            )
                            requireActivity().startActivity(intent)
                        }
                    )

                    if (selectedTab == HomeTab.FOR_YOU && !swipeToExplorePromptShown) {
                        val dismissSwipePrompt = {
                            swipeToExplorePromptShown = true
                            Prefs.isHomeSwipeToExplorePromptShown = true
                        }
                        WikipediaAlertDialog(
                            title = stringResource(R.string.explore_feed_swipe_to_explore_prompt_title),
                            titleModifier = Modifier.fillMaxWidth(),
                            message = stringResource(R.string.explore_feed_swipe_to_explore_prompt_message),
                            image = {
                                Image(
                                    painter = painterResource(R.drawable.illustration_swipe_gesture),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButtonText = stringResource(R.string.onboarding_got_it),
                            onDismissRequest = dismissSwipePrompt,
                            onConfirmButtonClick = dismissSwipePrompt
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.onTabChanged(NavTab.HOME)
        viewModel.updateTabCount()
        instrument.startFunnel("home_feed")
        refreshNotification()
        if (Prefs.homeForYouModulesToday.isEmpty()) {
            viewModel.refreshForYouContent()
        }
        // TODO: start new funnel for analytics
    }

    fun getCurrentTab(): HomeTab {
        return viewModel.selectedTab.value
    }

    override fun onPause() {
        super.onPause()
        cardImpressions.clear()
        instrument.stopFunnel()
    }

    fun refreshNotification() {
        viewModel.refreshUnreadNotificationCount()
    }

    fun selectTab(tab: HomeTab) {
        viewModel.selectTab(tab)
        (requireActivity() as? MainActivity)?.onTabChanged(NavTab.HOME)
    }

    private fun maybeShowExploreFeedUpdatePrompt() {
        if (!Prefs.isInitialOnboardingEnabled && Prefs.isExploreFeedUpdatePromptShown.not()) {
            personalizationResultLauncher.launch(ExploreFeedUpdatePromptActivity.newIntent(requireContext()))
        }
    }

    private fun onCardImpression(card: Card, index: Int) {
        if (cardImpressions.add(card.hideKey)) {
            instrument.submitInteraction(
                "impression",
                actionSource = card.javaClass.simpleName,
                actionContext = mapOf("card_index" to index)
            )
        }
    }
}

@Composable
fun HomeScreen(
    wikiSite: WikiSite,
    languageState: AppLanguageState? = null,
    selectedTab: HomeTab,
    communityContentState: CommunityContentState,
    forYouContentState: ForYouContentState,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    tabsState: TabsState,
    notificationBellState: NotificationBellState,
    onSelectTab: (HomeTab) -> Unit = {},
    onRefreshTab: (HomeTab) -> Unit = {},
    onLoadMoreCommunityContent: () -> Unit = {},
    onLoadMoreForYouContent: () -> Unit = {},
    onHideCommunityCardClick: (card: Card) -> Unit = {},
    onHideForYouCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: (moduleKey: String) -> Unit = {},
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageOverflowClick: (card: Card, pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onNewsClick: (card: NewsCard, newsItem: NewsItem) -> Unit = { _, _ -> },
    onImageClick: (card: FeaturedImageCard) -> Unit = {},
    onImageDownloadClick: (card: FeaturedImageCard) -> Unit = {},
    onImageShareClick: (card: FeaturedImageCard) -> Unit = {},
    onLanguageSelected: (String) -> Unit = {},
    onManageLanguagesClick: () -> Unit = {},
    onTabClick: () -> Unit = {},
    onUpdateTabCount: () -> Unit = {},
    onCustomizeInterestsClick: (card: Card) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onCardFooterClick: (card: Card) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onManageModulesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val topInset = if (context is MainActivity) {
        DimenUtil.roundedPxToDp((context.getStatusBarInsets()?.top ?: 0).toFloat())
    } else 64
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = pullToRefreshState.isAnimating && (communityContentState.isInitialLoading || forYouContentState.isInitialLoading)

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = { onRefreshTab(selectedTab) },
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                containerColor = WikipediaTheme.colors.paperColor,
                color = WikipediaTheme.colors.progressiveColor,
                state = pullToRefreshState
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedTab) {
                HomeTab.COMMUNITY -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxSize()
                            .background(WikipediaTheme.colors.paperColor)
                    ) {
                        HomeToolbar(
                            topInset = topInset,
                            tabsState = tabsState,
                            onTabClick = onTabClick,
                            onUpdateTabCount = onUpdateTabCount,
                            notificationBellState = notificationBellState,
                            onNotificationClick = onNotificationClick
                        )

                        HomeTabBar(
                            modifier = Modifier.padding(top = 8.dp),
                            wikiSite = wikiSite,
                            selectedTab = selectedTab,
                            languageState = languageState,
                            onTabSelected = onSelectTab,
                            onLanguageSelected = {
                                onLanguageSelected(it)
                            },
                            onManageLanguagesClick = {
                                onManageLanguagesClick()
                            }
                        )

                        CommunityContentTab(
                            modifier = Modifier.weight(1f),
                            wikiSite = wikiSite,
                            state = communityContentState,
                            overflowMenuState = overflowMenuState,
                            onLoadMore = onLoadMoreCommunityContent,
                            onHideCardClick = onHideCommunityCardClick,
                            onHideModuleClick = onHideModuleClick,
                            onPageClick = onPageClick,
                            onPageBookmarkClick = onPageBookmarkClick,
                            onPageShareClick = onPageShareClick,
                            onPageOverflowClick = onPageOverflowClick,
                            onPageOverflowDismiss = onPageOverflowDismiss,
                            onNewsClick = onNewsClick,
                            onImageClick = onImageClick,
                            onImageDownloadClick = onImageDownloadClick,
                            onImageShareClick = onImageShareClick,
                            onCardImpression = onCardImpression,
                            onCardFooterClick = onCardFooterClick,
                            onManageModulesClick = onManageModulesClick
                        )
                    }
                }

                HomeTab.FOR_YOU -> {
                    ForYouContentTab(
                        topInset = topInset,
                        state = forYouContentState,
                        wikiSite = wikiSite,
                        onLoadMore = onLoadMoreForYouContent,
                        overflowMenuState = overflowMenuState,
                        onPageClick = onPageClick,
                        onHideCardClick = onHideForYouCardClick,
                        onHideModuleClick = onHideModuleClick,
                        onPageBookmarkClick = onPageBookmarkClick,
                        onPageShareClick = onPageShareClick,
                        onPageOverflowClick = onPageOverflowClick,
                        onPageOverflowDismiss = onPageOverflowDismiss,
                        onCustomizeInterestsClick = onCustomizeInterestsClick,
                        onCardImpression = onCardImpression,
                        onManageModulesClick = onManageModulesClick
                    )

                    // Floating toolbar with gradient scrim, wordmark, and tab selector.
                    Column(modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.80f))
                        ) {
                            HomeToolbar(
                                topInset = topInset,
                                tabsState = tabsState,
                                onTabClick = onTabClick,
                                onUpdateTabCount = onUpdateTabCount,
                                notificationBellState = notificationBellState,
                                onNotificationClick = onNotificationClick
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.80f),
                                            0.18f to Color.Black.copy(alpha = 0.64f),
                                            0.38f to Color.Black.copy(alpha = 0.40f),
                                            0.58f to Color.Black.copy(alpha = 0.20f),
                                            0.76f to Color.Black.copy(alpha = 0.08f),
                                            0.90f to Color.Black.copy(alpha = 0.02f),
                                            1.0f to Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            HomeTabBar(
                                modifier = Modifier.padding(top = 8.dp, bottom = 64.dp),
                                wikiSite = wikiSite,
                                selectedTab = selectedTab,
                                languageState = languageState,
                                onTabSelected = onSelectTab,
                                onLanguageSelected = {
                                    onLanguageSelected(it)
                                },
                                onManageLanguagesClick = {
                                    onManageLanguagesClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeToolbar(
    topInset: Int,
    tabsState: TabsState,
    notificationBellState: NotificationBellState,
    onTabClick: () -> Unit,
    onUpdateTabCount: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Row {
        Image(
            painter = painterResource(R.drawable.feed_header_wordmark),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WikipediaTheme.colors.primaryColor),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, top = (topInset + 16).dp)
                .width(128.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        if (tabsState.count > 0) {
            IconButton(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = topInset.dp),
                onClick = { onTabClick() }
            ) {
                TabsBox(
                    modifier = Modifier
                        .width(21.dp)
                        .height(20.dp)
                        .then(
                            if (tabsState.pulse) {
                                Modifier.pulse(
                                    durationMillis = 300,
                                    toScale = 1.25f,
                                    onCompleted = {
                                        onUpdateTabCount()
                                    }
                                )
                            } else {
                                Modifier
                            }),
                    backgroundColor = Color.Transparent,
                    count = tabsState.count
                )
            }
        }
        if (notificationBellState.canShow) {
            NotificationBell(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = topInset.dp),
                unreadCount = notificationBellState.unreadCount,
                onClick = onNotificationClick
            )
        }
    }
}

@Composable
fun HomeTabBar(
    modifier: Modifier,
    wikiSite: WikiSite,
    selectedTab: HomeTab,
    languageState: AppLanguageState? = null,
    onTabSelected: (HomeTab) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onManageLanguagesClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f)
        ) {
            HomeTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val label = when (tab) {
                    HomeTab.COMMUNITY -> LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_tab_label)
                    HomeTab.FOR_YOU -> LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_for_you_tab_label)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selectedTab == HomeTab.FOR_YOU) WikipediaTheme.colors.primaryColor else if (isSelected) WikipediaTheme.colors.progressiveColor else WikipediaTheme.colors.primaryColor,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(
                                if (isSelected) {
                                    if (selectedTab == HomeTab.FOR_YOU) WikipediaTheme.colors.primaryColor
                                    else WikipediaTheme.colors.progressiveColor
                                } else Color.Transparent
                            )
                    )
                }
            }
        }
        LanguageDropDownMenu(
            selectedLanguageCode = wikiSite.languageCode,
            onLanguageSelected = { onLanguageSelected(it) },
            onManageLanguagesClick = { onManageLanguagesClick() },
            languageState = languageState
        )
    }
}

@Composable
fun CommunityContentTab(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    state: CommunityContentState,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onLoadMore: () -> Unit,
    onHideCardClick: (card: Card) -> Unit = {},
    onHideModuleClick: (moduleKey: String) -> Unit = {},
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageOverflowClick: (card: Card, pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onNewsClick: (card: NewsCard, newsItem: NewsItem) -> Unit = { _, _ -> },
    onImageClick: (card: FeaturedImageCard) -> Unit = {},
    onImageDownloadClick: (card: FeaturedImageCard) -> Unit = {},
    onImageShareClick: (card: FeaturedImageCard) -> Unit = {},
    onCardFooterClick: (card: Card) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onManageModulesClick: () -> Unit
) {
    when {
        state.isInitialLoading -> {
            LoadingIndicator(modifier = modifier.fillMaxHeight())
        }
        state.isEmptyState -> {
            val context = LocalContext.current
            HomeScreenEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                title = context.getString(wikiSite.languageCode, R.string.home_feed_screen_empty_state_label),
                description = context.getString(wikiSite.languageCode, R.string.home_feed_community_screen_empty_state_description),
                onManageModulesClick = onManageModulesClick
            )
        }
        state.error != null && state.cards.isEmpty() -> {
            ErrorState(state.error, onRetry = onLoadMore)
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
            ) {
                item {
                    CommunityDisclaimer(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        wikiSite = wikiSite
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                var lastCardWasDayHeader = false
                state.cards.forEachIndexed { cardIndex, card ->
                    when (card) {
                        is DayHeaderCard -> {
                            item(key = "day-header-${card.age}") {
                                DayHeader(LocalDate.now().minusDays(card.age.toLong()), isFirst = card.age == 0)
                            }
                        }
                        is FeaturedArticleCard -> {
                            item(key = "tfa-${card.age}") {
                                FeaturedArticleModule(
                                    wikiSite = wikiSite,
                                    card.page,
                                    onPageClick = {
                                        onPageClick(card,
                                            it.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_FEATURED
                                            )
                                        )
                                    },
                                    onHideCardClick = { onHideCardClick(card) },
                                    onHideModuleClick = {
                                        onHideModuleClick(card.moduleKey())
                                    },
                                    onShareClick = {
                                        onPageShareClick(card,
                                            it.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_FEATURED
                                            )
                                        )
                                    },
                                    onBookmarkClick = {
                                        onPageBookmarkClick(card,
                                            it.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_FEATURED
                                            )
                                        )
                                    },
                                    onCardImpression = { onCardImpression(card, cardIndex) }
                                )
                            }
                        }
                        is TopReadListCard -> {
                            if (lastCardWasDayHeader) {
                                item(key = "top-read-spacer-${card.age}") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            item(key = "top-read-${card.age}") {
                                TopReadModule(
                                    wikiSite = wikiSite,
                                    topRead = card.articles,
                                    pageOverflowContent = { index ->
                                        PageOverflowMenu(
                                            menuKey = "top-read-${card.age}-$index",
                                            overflowMenuState = overflowMenuState,
                                            onDismiss = onPageOverflowDismiss,
                                            items = overflowMenuState?.items.orEmpty()
                                        )
                                    },
                                    onHideCardClick = { onHideCardClick(card) },
                                    onHideModuleClick = {
                                        onHideModuleClick(card.moduleKey())
                                    },
                                    onPageClick = { entry ->
                                        onPageClick(card,
                                            entry.getHistoryEntry(
                                                wikiSite,
                                                HistoryEntry.SOURCE_FEED_MOST_READ
                                            )
                                        )
                                    },
                                    onPageOverflowClick = { pageSummary, index ->
                                        onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_MOST_READ, "top-read-${card.age}-$index")
                                    },
                                    onFooterClick = { onCardFooterClick(card) },
                                    onCardImpression = { onCardImpression(card, cardIndex) }
                                )
                            }
                        }
                        is NewsCard -> {
                            item(key = "news-${card.age}") {
                                NewsModule(
                                    wikiSite = wikiSite,
                                    newsItems = card.news,
                                    onHideCardClick = { onHideCardClick(card) },
                                    onHideModuleClick = {
                                        onHideModuleClick(card.moduleKey())
                                    },
                                    onNewsClick = { newsItem ->
                                        onNewsClick(card, newsItem)
                                    },
                                    onCardImpression = { onCardImpression(card, cardIndex) }
                                )
                            }
                        }
                        is OnThisDayCard -> {
                            if (lastCardWasDayHeader) {
                                item(key = "on-this-day-spacer-${card.age}") {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                            item(key = "on-this-day-${card.age}") {
                                OnThisDayModule(
                                    wikiSite = wikiSite,
                                    events = card.events,
                                    pageOverflowContent = { eventIndex, itemIndex ->
                                        PageOverflowMenu(
                                            menuKey = "on-this-day-${card.age}-$eventIndex-$itemIndex",
                                            overflowMenuState = overflowMenuState,
                                            onDismiss = onPageOverflowDismiss,
                                            items = overflowMenuState?.items.orEmpty()
                                        )
                                    },
                                    onHideCardClick = { onHideCardClick(card) },
                                    onHideModuleClick = {
                                        onHideModuleClick(card.moduleKey())
                                    },
                                    onPageClick = { pageSummary ->
                                        onPageClick(card, pageSummary.getHistoryEntry(wikiSite, HistoryEntry.SOURCE_FEED_ON_THIS_DAY))
                                    },
                                    onPageOverflowClick = { pageSummary, eventIndex, itemIndex ->
                                        onPageOverflowClick(card, pageSummary, HistoryEntry.SOURCE_FEED_ON_THIS_DAY, "on-this-day-${card.age}-$eventIndex-$itemIndex")
                                    },
                                    onFooterClick = { onCardFooterClick(card) },
                                    onCardImpression = { onCardImpression(card, cardIndex) }
                                )
                            }
                        }
                        is FeaturedImageCard -> {
                            item(key = "tfi-${card.age}") {
                                FeaturedImageModule(
                                    wikiSite = wikiSite,
                                    card = card,
                                    onHideCardClick = { onHideCardClick(card) },
                                    onHideModuleClick = {
                                        onHideModuleClick(card.moduleKey())
                                    },
                                    onClick = onImageClick,
                                    onDownloadClick = onImageDownloadClick,
                                    onShareClick = onImageShareClick,
                                    onCardImpression = { onCardImpression(card, cardIndex) }
                                )
                            }
                        }
                        else -> {
                            // TODO: Today's Featured Picture
                            // TODO: DYK
                            // TODO: Media of the day (Commons)
                        }
                    }
                    lastCardWasDayHeader = card is DayHeaderCard
                }

                item(key = "load-more-community") {
                    if (state.isLoadingMore) {
                        LoadingIndicator()
                    } else if (state.canLoadMore && state.cards.isNotEmpty()) {
                        LoadMoreButton(
                            wikiSite = wikiSite,
                            isCommunity = true,
                            onClick = onLoadMore
                        )
                    }
                }

                if (state.error != null && state.cards.isNotEmpty()) {
                    item(key = "error-community") {
                        ErrorState(state.error, onRetry = onLoadMore)
                    }
                }
            }
        }
    }
}

@Composable
fun ForYouContentTab(
    state: ForYouContentState,
    topInset: Int,
    wikiSite: WikiSite,
    onLoadMore: () -> Unit,
    overflowMenuState: PageOverflowMenuViewModel.PageOverflowMenuState? = null,
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: (moduleKey: String) -> Unit = {},
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageOverflowClick: (card: Card, pageSummary: PageSummary, source: Int, menuKey: String) -> Unit = { _, _, _, _ -> },
    onPageOverflowDismiss: () -> Unit = {},
    onCustomizeInterestsClick: (card: Card) -> Unit = {},
    onCardImpression: (card: Card, index: Int) -> Unit = { _, _ -> },
    onManageModulesClick: () -> Unit
) {
    when {
        state.isInitialLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = WikipediaTheme.colors.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(modifier = Modifier.fillMaxHeight())
            }
        }
        state.isEmptyState -> {
            val context = LocalContext.current
            HomeScreenEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WikipediaTheme.colors.paperColor)
                    .padding(horizontal = 16.dp)
                    .padding(top = (topInset * 2 + 64).dp)
                    .verticalScroll(rememberScrollState()),
                title = context.getString(wikiSite.languageCode, R.string.home_feed_screen_empty_state_label),
                description = context.getString(wikiSite.languageCode, R.string.home_feed_for_you_screen_empty_state_description),
                onManageModulesClick = onManageModulesClick
            )
        }
        state.error != null && state.modules.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = WikipediaTheme.colors.backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                ErrorState(state.error, onRetry = onLoadMore)
            }
        }
        else -> {
            val listState = rememberLazyListState()
            val modules = state.modules

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportHeight = maxHeight

                LazyColumn(
                    state = listState,
                    flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WikipediaTheme.colors.backgroundColor)
                ) {
                    modules.forEachIndexed { index, module ->
                        when (module) {
                            is ForYouModule.BasedOnInterest -> {
                                item(key = "interest-${module.age}-$index") {
                                    BasedOnInterestModule(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(viewportHeight),
                                        wikiSite = wikiSite,
                                        module = module,
                                        onPageClick = onPageClick,
                                        onPageShareClick = onPageShareClick,
                                        onPageBookmarkClick = onPageBookmarkClick,
                                        onHideCardClick = onHideCardClick,
                                        onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                                        onCardInView = { onCardImpression(it, index) },
                                        onCustomizeInterestsClick = onCustomizeInterestsClick
                                    )
                                }
                            }

                            is ForYouModule.ContinueReading -> {
                                item(key = "continue-reading-${module.age}-$index") {
                                    ContinueReadingModule(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(viewportHeight),
                                        wikiSite = wikiSite,
                                        module = module,
                                        onPageClick = onPageClick,
                                        onPageShareClick = onPageShareClick,
                                        onPageBookmarkClick = onPageBookmarkClick,
                                        onHideCardClick = onHideCardClick,
                                        onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                                        onCardInView = { onCardImpression(it, index) },
                                        onCustomizeInterestsClick = onCustomizeInterestsClick
                                    )
                                }
                            }

                            is ForYouModule.BecauseYouRead -> {
                                item(key = "because-you-read-${module.age}-$index") {
                                    BecauseYouReadModule(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(viewportHeight),
                                        wikiSite = wikiSite,
                                        module = module,
                                        onPageClick = onPageClick,
                                        onPageShareClick = onPageShareClick,
                                        onPageBookmarkClick = onPageBookmarkClick,
                                        onHideCardClick = onHideCardClick,
                                        onHideModuleClick = { onHideModuleClick(module.moduleKey()) },
                                        onCardInView = { onCardImpression(it, index) },
                                        onCustomizeInterestsClick = onCustomizeInterestsClick
                                    )
                                }
                            }
                        }
                    }

                    item(key = "load-more-foryou") {
                        if (state.isLoadingMore) {
                            LoadingIndicator()
                        } else if (state.canLoadMore) {
                            // In case we want to load more For You items in the future:
                            // LoadMoreButton(
                            //     wikiSite = wikiSite,
                            //     isCommunity = false,
                            //     onClick = onLoadMore
                            // )
                        }
                    }

                    if (state.error != null && state.modules.isNotEmpty()) {
                        item(key = "error-foryou") {
                            ErrorState(state.error, onRetry = onLoadMore)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommunityDisclaimer(
    modifier: Modifier,
    wikiSite: WikiSite
) {
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = WikipediaTheme.colors.borderColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
            Image(
                modifier = Modifier.size(45.dp),
                painter = painterResource(R.drawable.w_nav_mark),
                contentDescription = null
            )
        }
    }
}
@Composable
fun DayHeader(date: LocalDate, isFirst: Boolean = true) {
    val dateFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(LocalLocale.current.platformLocale, "MMM dd, yyyy"))
    Text(
        text = if (LocalDate.now().dayOfYear == date.dayOfYear) stringResource(R.string.explore_feed_date_today, date.format(dateFormatter)) else date.format(dateFormatter),
        color = WikipediaTheme.colors.secondaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (isFirst) 16.dp else 24.dp)
    )
}

@Composable
fun LoadMoreButton(
    wikiSite: WikiSite,
    isCommunity: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isCommunity) {
            AppButton(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                onClick = onClick,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dynamic_feed_24dp),
                        tint = WikipediaTheme.colors.paperColor,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_load_more_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onClick) {
                    Text(
                        text = "Load more recommendations",
                        color = WikipediaTheme.colors.progressiveColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = WikipediaTheme.colors.progressiveColor
        )
    }
}

@Composable
fun ErrorState(caught: Throwable, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WikiErrorView(
            modifier = Modifier,
            caught,
            errorClickEvents = WikiErrorClickEvents(
                retryClickListener = {
                    onRetry()
                }
            ),
            retryForGenericError = true
        )
    }
}

@Composable
fun LanguageDropDownMenu(
    selectedLanguageCode: String,
    onLanguageSelected: (String) -> Unit,
    onManageLanguagesClick: () -> Unit,
    languageState: AppLanguageState? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                expanded = true
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = WikipediaTheme.colors.primaryColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WikiLangCodeBox(
                modifier = Modifier
                    .height(20.dp)
                    .widthIn(min = 20.dp),
                languageCode = selectedLanguageCode,
                backgroundColor = WikipediaTheme.colors.primaryColor.copy(alpha = 0.8f),
                borderColor = Color.Transparent,
                textColor = WikipediaTheme.colors.paperColor,
            )
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_arrow_down_24),
                contentDescription = null,
                tint = WikipediaTheme.colors.primaryColor
            )
        }
        DropdownMenu(
            expanded = expanded,
            containerColor = WikipediaTheme.colors.paperColor,
            onDismissRequest = { expanded = false }
        ) {
            val languageCodes = languageState?.appLanguageCodes.orEmpty()
            repeat(languageCodes.size) {
                val langCode = languageCodes[it]
                DropdownMenuItem(
                    leadingIcon = {
                        WikiLangCodeBox(
                            modifier = Modifier
                                .height(20.dp)
                                .widthIn(min = 20.dp),
                            languageCode = langCode,
                            borderColor = WikipediaTheme.colors.secondaryColor,
                            textColor = WikipediaTheme.colors.secondaryColor,
                        )
                    },
                    trailingIcon = {
                        if (langCode == selectedLanguageCode) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check_black_24dp),
                            contentDescription = null,
                            tint = WikipediaTheme.colors.secondaryColor
                        )
                            }
                    },
                    text = {
                        Text(
                            text = languageState?.getAppLanguageLocalizedName(langCode) ?: langCode,
                            style = MaterialTheme.typography.bodyLarge,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    },
                    onClick = {
                        onLanguageSelected(langCode)
                        expanded = false
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = WikipediaTheme.colors.borderColor
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onManageLanguagesClick()
                        expanded = false
                    }
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = LocalContext.current.getString(selectedLanguageCode, R.string.explore_feed_manage_languages_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            }
        }
    }
}

@Composable
fun HomeScreenEmptyState(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    onManageModulesClick: () -> Unit,
) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier
                .padding(bottom = 16.dp),
            painter = painterResource(R.drawable.empty_feed_illustration),
            contentDescription = null
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15.sp
            ),
            color = WikipediaTheme.colors.secondaryColor
        )
        HtmlText(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                letterSpacing = 0.25.sp
            ),
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.secondaryColor
        )
        AppButton(
            onClick = onManageModulesClick
        ) {
            Text(
                text = stringResource(R.string.home_feed_screen_empty_state_btn_label)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenCommunityEmptyStatePreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.COMMUNITY,
            communityContentState = CommunityContentState(isEmptyState = true),
            forYouContentState = ForYouContentState(isInitialLoading = true),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 5, canShow = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenForYouEmptyStatePreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.FOR_YOU,
            communityContentState = CommunityContentState(isEmptyState = true),
            forYouContentState = ForYouContentState(isEmptyState = true),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 5, canShow = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenCommunityPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.COMMUNITY,
            communityContentState = CommunityContentState(isInitialLoading = true),
            forYouContentState = ForYouContentState(isInitialLoading = true),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 5, canShow = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenForYouPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeScreen(
            wikiSite = WikiSite.preview(),
            selectedTab = HomeTab.FOR_YOU,
            communityContentState = CommunityContentState(isInitialLoading = true),
            forYouContentState = ForYouContentState(isInitialLoading = true),
            tabsState = TabsState(1, false),
            notificationBellState = NotificationBellState(unreadCount = 99, canShow = true)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun CommunityDisclaimerPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CommunityDisclaimer(
            modifier = Modifier
                .padding(16.dp)
                .height(72.dp),
            wikiSite = WikiSite.preview()
        )
    }
}

@Preview
@Composable
fun LoadMoreButtonPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LoadMoreButton(
            wikiSite = WikiSite.preview(),
            isCommunity = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun LanguageDropDownMenuPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LanguageDropDownMenu(
            selectedLanguageCode = "en",
            onLanguageSelected = {},
            onManageLanguagesClick = {}
        )
    }
}

@Preview
@Composable
fun DayHeaderPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        DayHeader(LocalDate.now())
    }
}
