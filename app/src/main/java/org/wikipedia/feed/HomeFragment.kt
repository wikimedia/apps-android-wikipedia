package org.wikipedia.feed

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.testkitchen.TestKitchenAdapter
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.components.menu.PageOverflowMenuViewModel
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.didyouknow.DidYouKnowActivity
import org.wikipedia.feed.didyouknow.DidYouKnowCard
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.DiscoverCard
import org.wikipedia.feed.model.EmptyCommunityCard
import org.wikipedia.feed.model.EmptyForYouCard
import org.wikipedia.feed.model.GamesModulePromptCard
import org.wikipedia.feed.model.PlacesOfInterestLocationPromptCard
import org.wikipedia.feed.model.WikiGameCard
import org.wikipedia.feed.onboarding.ExploreFeedUpdatePromptActivity
import org.wikipedia.feed.onthisday.OnThisDayActivity
import org.wikipedia.feed.onthisday.OnThisDayCard
import org.wikipedia.feed.personalization.PersonalizationActivity
import org.wikipedia.feed.personalization.PersonalizationActivity.Companion.RESULT_INTERESTS_UPDATED
import org.wikipedia.feed.personalization.homepreference.HomePreferenceType
import org.wikipedia.feed.topread.TopReadArticlesActivity
import org.wikipedia.feed.topread.TopReadCard
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.feed.wikigames.WikiGame
import org.wikipedia.games.GamesHubActivity
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.main.MainActivity
import org.wikipedia.main.MainFragment
import org.wikipedia.navtab.NavTab
import org.wikipedia.notifications.NotificationActivity
import org.wikipedia.page.tabs.TabActivity
import org.wikipedia.places.PlacesActivity
import org.wikipedia.random.RandomActivity
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode
import org.wikipedia.readinglist.recommended.RecommendedReadingListOnboardingActivity
import org.wikipedia.readinglist.recommended.RecommendedReadingListSettingsActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.homefeed.HomeFeedSettingsActivity
import org.wikipedia.settings.homefeed.HomeFeedSettingsStartDestination
import org.wikipedia.settings.languages.WikipediaLanguagesActivity
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.SurveyDialog
import java.time.LocalDate

class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()
    private val pageOverflowMenuViewModel: PageOverflowMenuViewModel by viewModels()
    private val cardImpressions = mutableSetOf<String>()
    private val instrument = TestKitchenAdapter.client.getInstrument("apps-home-feed").startFunnel("home_feed")

    private val personalizationResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val tab = if (Prefs.homePreferenceSelection == HomePreferenceType.PERSONALIZED) HomeTab.FOR_YOU else HomeTab.COMMUNITY
            selectTab(tab)
        }
    }

    private val customizeInterestsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_INTERESTS_UPDATED) {
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
                val forYouContentState by viewModel.forYouState.collectAsState()
                val communityContentState by viewModel.communityState.collectAsState()
                var swipeToExplorePromptShown by remember { mutableStateOf(Prefs.isHomeSwipeToExplorePromptShown) }

                BaseTheme(currentTheme = if (selectedTab == HomeTab.FOR_YOU) Theme.BLACK else WikipediaApp.instance.currentTheme) {
                    HomeScreen(
                        wikiSite = wikiSite,
                        languageState = WikipediaApp.instance.languageState,
                        selectedTab = selectedTab,
                        communityContentState = communityContentState,
                        forYouContentState = forYouContentState,
                        overflowMenuState = pageOverflowMenuViewModel.pageOverflowMenuState,
                        tabsState = tabsState,
                        notificationBellState = notificationState,
                        onAction = { handleHomeAction(it, wikiSite, selectedTab) }
                    )

                    if (selectedTab == HomeTab.FOR_YOU && !swipeToExplorePromptShown && forYouContentState.modules.isNotEmpty()) {
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
                                    painter = painterResource(R.drawable.swipe_gesture_illustration),
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
        viewModel.updateSelectedLanguageIfNeeded()
        instrument.startFunnel("home_feed")
        refreshNotification()
        maybeShowSurveyDialog()
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

    fun updateLanguage(languageCode: String) {
        viewModel.updateLanguage(languageCode)
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

    private fun handleHomeAction(action: HomeAction, wikiSite: WikiSite, selectedTab: HomeTab) {
        when (action) {
            is HomeAction.SelectTab -> {
                val card = action.card
                if (card is EmptyCommunityCard || card is EmptyForYouCard) {
                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "community_feed")
                }
                selectTab(action.tab)
            }
            is HomeAction.RefreshTab -> {
                Prefs.homeForYouModulesToday = ""
                if (action.tab == HomeTab.COMMUNITY) {
                    viewModel.refreshCommunityContent()
                } else {
                    viewModel.refreshForYouContent()
                }
            }
            HomeAction.LoadMoreCommunityContent -> viewModel.loadCommunityContent()
            HomeAction.LoadMoreForYouContent -> viewModel.loadForYouContent()
            is HomeAction.HideCommunityCard -> {
                val card = action.card
                instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "card_hide")
                viewModel.hideCommunityCard(card)
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed))
                    .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                        instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "undo_card_hide")
                        viewModel.restoreCommunityCard(card)
                    }.show()
            }
            is HomeAction.HideForYouCard -> {
                val card = action.card
                instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "card_hide")
                viewModel.hideForYouCard(card)
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.menu_feed_card_dismissed))
                    .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                        instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, actionSubtype = "feed_overflow", elementId = "undo_card_hide")
                        viewModel.restoreForYouCard(card)
                    }.show()
            }
            is HomeAction.HideModule -> {
                val moduleKey = action.moduleKey
                instrument.submitInteraction("click", actionSource = moduleKey, actionSubtype = "feed_overflow", elementId = "module_hide")
                viewModel.hideModule(moduleKey)
                FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.explore_feed_header_overflow_hide_module_message))
                    .setAction(getString(R.string.explore_feed_header_overflow_hide_module_message_action)) {
                        instrument.submitInteraction("click", actionSource = moduleKey, actionSubtype = "feed_overflow", elementId = "undo_module_hide")
                        viewModel.restoreModule(moduleKey)
                    }.show()
            }
            is HomeAction.PageClick -> {
                instrument.submitInteraction("click", actionSource = action.card.javaClass.simpleName, elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = action.historyEntry.title))
                (parentFragment as? MainFragment)?.onFeedSelectPage(action.historyEntry, false)
            }
            is HomeAction.PageBookmarkClick -> {
                instrument.submitInteraction("click", actionSource = action.card.javaClass.simpleName, elementId = "article_save", pageData = TestKitchenAdapter.getPageData(pageTitle = action.historyEntry.title))
                (parentFragment as? MainFragment)?.onFeedAddPageToList(action.historyEntry, true)
            }
            is HomeAction.PageShareClick -> {
                instrument.submitInteraction("click", actionSource = action.card.javaClass.simpleName, elementId = "article_share", pageData = TestKitchenAdapter.getPageData(pageTitle = action.historyEntry.title))
                ShareUtil.shareText(requireContext(), action.historyEntry.title)
            }
            is HomeAction.PageOverflowClick -> {
                val card = action.card
                pageOverflowMenuViewModel.onPageOverflowClick(
                    context = requireContext(),
                    wikiSite = wikiSite,
                    pageSummary = action.pageSummary,
                    source = action.source,
                    menuKey = action.menuKey,
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
            }
            HomeAction.PageOverflowDismiss -> {
                pageOverflowMenuViewModel.dismissPageOverflowMenu()
            }
            is HomeAction.NewsClick -> {
                val card = action.card
                instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = card.javaClass.simpleName, actionContext = mapOf("index" to card.news.indexOf(action.newsItem)))
                (parentFragment as? MainFragment)?.onFeedNewsItemSelected(action.newsItem, wikiSite)
            }
            is HomeAction.ImageClick -> {
                val card = action.card
                instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "article_open", pageData = TestKitchenAdapter.getPageData(pageTitle = card.featuredImage.toPageTitle()))
                (parentFragment as? MainFragment)?.onFeaturedImageSelected(card.featuredImage)
            }
            is HomeAction.ImageShareClick -> {
                val card = action.card
                instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "share", pageData = TestKitchenAdapter.getPageData(pageTitle = card.featuredImage.toPageTitle()))
                (parentFragment as? MainFragment)?.onFeedShareImage(card.featuredImage, card.age)
            }
            is HomeAction.ImageDownloadClick -> {
                val card = action.card
                instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "download", pageData = TestKitchenAdapter.getPageData(pageTitle = card.featuredImage.toPageTitle()))
                (parentFragment as? MainFragment)?.onFeedDownloadImage(card.featuredImage)
            }
            is HomeAction.LanguageSelected -> {
                instrument.submitInteraction("click", "language_menu", elementId = "language_change", actionContext = mapOf("selected_tab" to selectedTab.name, "language_code" to action.languageCode))
                updateLanguage(action.languageCode)
            }
            HomeAction.ManageLanguagesClick -> {
                instrument.submitInteraction("click", "language_menu", elementId = "manage_languages", actionContext = mapOf("selected_tab" to selectedTab.name))
                requireActivity().startActivity(WikipediaLanguagesActivity.newIntent(requireContext(), invokeSource = InvokeSource.FEED))
            }
            is HomeAction.CustomizeClick -> {
                val card = action.card
                if (card != null) {
                    instrument.submitInteraction("click", actionSource = card.javaClass.simpleName,
                        actionSubtype = if (card !is EmptyForYouCard) "feed_overflow" else null,
                        elementId = "feed_customize")
                }
                if (card is DiscoverCard) {
                    requireActivity().startActivity(RecommendedReadingListSettingsActivity.newIntent(requireContext()))
                } else {
                    customizeInterestsLauncher.launch(PersonalizationActivity.newIntent(requireContext(), showInterestsOnly = true))
                }
            }
            HomeAction.TabClick -> {
                requireActivity().startActivity(TabActivity.newIntent(requireActivity()))
            }
            HomeAction.UpdateTabCount -> {
                viewModel.updateTabCount(false)
            }
            is HomeAction.CardImpression -> {
                onCardImpression(action.card, action.index)
            }
            is HomeAction.CardFooterClick -> {
                when (val card = action.card) {
                    is TopReadCard -> {
                        instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "more_top_read")
                        startActivity(TopReadArticlesActivity.newIntent(requireActivity(), TopReadCard(card.articles, card.age, wikiSite)))
                    }
                    is OnThisDayCard -> {
                        instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "more_on_this_day")
                        startActivity(OnThisDayActivity.newIntent(requireActivity(), card.age, -1, wikiSite, InvokeSource.ON_THIS_DAY_CARD_FOOTER))
                    }
                    is DidYouKnowCard -> {
                        instrument.submitInteraction("click", actionSource = card.javaClass.simpleName, elementId = "more_did_you_know")
                        startActivity(DidYouKnowActivity.newIntent(requireActivity(), card.site, card.items))
                    }
                }
            }
            HomeAction.NotificationClick -> {
                requireActivity().startActivity(NotificationActivity.newIntent(requireActivity()))
            }
            HomeAction.ManageModulesClick -> {
                instrument.submitInteraction("click", actionSource = "feed_empty", elementId = "customize_feed")
                val intent = HomeFeedSettingsActivity.newIntent(
                    context = requireContext(),
                    startDestination = when (selectedTab) {
                        HomeTab.COMMUNITY -> HomeFeedSettingsStartDestination.COMMUNITY_MODULES
                        HomeTab.FOR_YOU -> HomeFeedSettingsStartDestination.FOR_YOU_MODULES
                    }
                )
                requireActivity().startActivity(intent)
            }
            HomeAction.ShuffleClick -> {
                instrument.submitInteraction("click", elementId = "random_card_shuffle_button")
                startActivity(RandomActivity.newIntent(requireActivity(), wikiSite, InvokeSource.FEED))
            }
            HomeAction.PlacesTeaserClick -> {
                instrument.submitInteraction("click", actionSource = PlacesOfInterestLocationPromptCard::class.java.simpleName, elementId = "go_to_places")
                requireActivity().startActivity(PlacesActivity.newIntent(requireContext()))
            }
            HomeAction.DiscoverTeaserClick -> {
                instrument.submitInteraction("click", elementId = "enable_discover_reading_list_button")
                requireActivity().startActivity(RecommendedReadingListOnboardingActivity.newIntent(requireContext()))
            }
            HomeAction.SeeAllRecommendationsClick -> {
                instrument.submitInteraction("click", elementId = "explore_all_recommendations_button")
                startActivity(ReadingListActivity.newIntent(requireContext(), readingListMode = ReadingListMode.RECOMMENDED))
            }
            is HomeAction.GameActionClick -> {
                when (val wikiGame = action.wikiGame) {
                    is WikiGame.OnThisDayGame -> {
                        val gameStatus = when (wikiGame.state) {
                            is OnThisDayCardGameState.Completed -> DailyGameHistory.GAME_COMPLETED
                            is OnThisDayCardGameState.InProgress,
                            is OnThisDayCardGameState.Preview -> DailyGameHistory.GAME_IN_PROGRESS
                        }
                        val elementId = when (wikiGame.state) {
                            is OnThisDayCardGameState.Preview -> "play_click"
                            is OnThisDayCardGameState.InProgress -> "continue_click"
                            is OnThisDayCardGameState.Completed -> "review_click"
                        }
                        instrument.submitInteraction("click", actionSource = WikiGameCard::class.java.simpleName, elementId = elementId, actionContext = mapOf("game" to wikiGame.game.name))
                        requireActivity().startActivity(OnThisDayGameActivity.newIntent(
                            context = requireContext(),
                            invokeSource = InvokeSource.FEED,
                            wikiSite = wikiSite,
                            gameStatus = gameStatus
                        ))
                    }
                }
            }
            HomeAction.GoToGamesHubClick -> {
                instrument.submitInteraction("click", actionSource = GamesModulePromptCard::class.java.simpleName, elementId = "go_to_games_hub")
                requireActivity().startActivity(GamesHubActivity.newIntent(requireContext()))
            }
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

    private fun maybeShowSurveyDialog() {
        val endDate = LocalDate.of(2026, 8, 14)
        val minVisits = 4

        // don't show the survey if the user has not seen other dialogs or prompts that can be shown on the feed
        if (!Prefs.isExploreFeedUpdatePromptShown || !Prefs.isHomeFeedUpdateTooltipShown) {
            return
        }

        if (Prefs.homeFeedSurveyShown || LocalDate.now().isAfter(endDate)) {
            return
        }

        if (Prefs.exploreFeedVisitCount < minVisits) {
            return
        }

        Prefs.homeFeedSurveyShown = true
        SurveyDialog.showHomeFeedFeedbackDialog(
            activity = requireActivity(),
            onImpression = {
                instrument.submitInteraction(
                    action = "impression",
                    actionSource = "home_feed_feedback"
                )
            },
            onCancel = {
                instrument.submitInteraction(
                    action = "click",
                    actionSource = "home_feed_feedback",
                    elementId = "feedback_cancel"
                )
            },
            onSubmit = { feedbackOption, feedbackText ->
                instrument.submitInteraction(
                    action = "click",
                    actionSource = "home_feed_feedback",
                    elementId = "feedback_submit",
                    actionContext = buildMap {
                        feedbackOption?.let { put("score", it) }
                        put("text", feedbackText)
                    }
                )
            }
        )
    }
}
