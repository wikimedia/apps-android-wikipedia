package org.wikipedia.activitytab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.activitytab.timeline.ActivitySource
import org.wikipedia.activitytab.timeline.TimelineItem
import org.wikipedia.analytics.eventplatform.ActivityTabEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.CategoryActivity
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.dataclient.mwapi.MwNotLoggedInException
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.events.LoggedInEvent
import org.wikipedia.events.LoggedOutEvent
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.games.GamesHubActivity
import org.wikipedia.games.WikiGames
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.history.HistoryEntry
import org.wikipedia.history.HistoryFragment
import org.wikipedia.login.LoginActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.SuggestedEditsTasksActivity
import org.wikipedia.usercontrib.UserContribListActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.UriUtil
import org.wikipedia.widgets.readingchallenge.ReadingChallengeRewardDialog
import org.wikipedia.widgets.readingchallenge.ReadingChallengeWidgetRepository
import org.wikipedia.yearinreview.presentation.YearInReviewViewModel

class ActivityTabFragment : Fragment() {
    interface Callback {
        fun onNavigateTo(navTab: NavTab)
    }

    private val viewModel: ActivityTabViewModel by viewModels()
    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_activity_tab_overflow, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return handleMenuItemClick(menuItem)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        is LoggedInEvent, is LoggedOutEvent, is LoggedOutInBackgroundEvent -> viewModel.loadAll()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.allDataLoaded.collectLatest {
                    if (it) {
                        val isAllDataEmpty = viewModel.hasNoReadingHistoryData() &&
                                viewModel.hasNoImpactData() &&
                                viewModel.hasNoGameStats() &&
                                viewModel.hasNoDonationData()
                        ActivityTabEvent.submit(
                            activeInterface = "activity_tab",
                            action = "impression",
                            editCount = viewModel.getTotalEditsCount(),
                            state = if (isAllDataEmpty) "empty" else "complete"
                        )
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.impactUiState.collectLatest {
                    if (it is UiState.Error && it.error is MwNotLoggedInException) {
                        AccountUtil.bailWithLogout()
                        requireActivity().window.decorView.post {
                            if (!requireActivity().isDestroyed) {
                                callback()?.onNavigateTo(NavTab.HOME)
                            }
                        }
                    }
                }
            }
        }
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    // Collected outside the login branch so that loadAll() after a login/logout recomposes this and re-checks the account state.
                    val readingHistoryState = viewModel.readingHistoryState.collectAsState().value
                    if (AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount) {
                        LoggedInScreen(readingHistoryState)
                    } else {
                        ActivityTabLoggedOutScreen(
                            onCreateAccountClick = {
                                ActivityTabEvent.submit(activeInterface = "activity_tab_login", action = "create_account_click")
                                startActivity(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_ACTIVITY_TAB))
                            },
                            onLoginClick = {
                                ActivityTabEvent.submit(activeInterface = "activity_tab_login", action = "login_click")
                                startActivity(LoginActivity.newIntent(requireContext(), LoginActivity.SOURCE_ACTIVITY_TAB, createAccountFirst = false))
                            },
                            showYearInReviewCard = YearInReviewViewModel.canShowEntryPoint,
                            onYirGetStartedClick = {
                                // TODO: add yir announcement activity
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        if (requireActivity().intent.getBooleanExtra(Constants.INTENT_EXTRA_SCROLL_TO_GAMES, false)) {
            viewModel.onScrollToGames()
            requireActivity().intent.removeExtra(Constants.INTENT_EXTRA_SCROLL_TO_GAMES)
        }
        if (!Prefs.isGameStatsUnavailableSnackbarShown) {
            requireActivity().intent.getStringExtra(Constants.INTENT_EXTRA_SNACKBAR_MESSAGE)?.let {
                FeedbackUtil.makeSnackbar(requireView(), it).show()
                requireActivity().intent.removeExtra(Constants.INTENT_EXTRA_SNACKBAR_MESSAGE)
                Prefs.isGameStatsUnavailableSnackbarShown = true
            }
        }
        maybeShowReadingChallengeRewardDialog()
        viewModel.loadAll()
        requireActivity().invalidateOptionsMenu()
    }

    private fun maybeShowReadingChallengeRewardDialog() {
        val intent = requireActivity().intent
        if (ReadingChallengeWidgetRepository.shouldShowReward(intent)) {
            intent.removeExtra(ReadingChallengeWidgetRepository.INTENT_EXTRA_READING_CHALLENGE_REWARD)
            ExclusiveBottomSheetPresenter.show(childFragmentManager, ReadingChallengeRewardDialog())
        }
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    @Composable
    private fun LoggedInScreen(readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>) {
        val userName = AccountUtil.userName
        val yearInReviewEntryState = viewModel.yearInReviewEntryState.collectAsState().value
        ActivityTabLoggedInScreen(
            userName = userName,
            languageCode = WikipediaApp.instance.wikiSite.languageCode,
            modules = Prefs.activityTabModules,
            haveAtLeastOneDonation = Prefs.donationResults.isNotEmpty(),
            areGamesAvailable = WikiGames.WHICH_CAME_FIRST.isLangSupported(WikipediaApp.instance.wikiSite.languageCode),
            refreshSilently = viewModel.shouldRefreshTimelineSilently,
            scrollToGames = viewModel.scrollToGames.collectAsState().value,
            readingHistoryState = readingHistoryState,
            donationUiState = viewModel.donationUiState.collectAsState().value,
            wikiGamesUiState = viewModel.wikiGamesUiState.collectAsState().value,
            impactUiState = viewModel.impactUiState.collectAsState().value,
            timelineFlow = viewModel.timelineFlow,
            onScrollToGamesConsumed = { viewModel.onScrollToGamesConsumed() },
            onRefresh = {
                viewModel.shouldRefreshTimelineSilently = false
                viewModel.loadAll()
            },
            onCustomizeClick = {
                startActivity(ActivityTabCustomizationActivity.newIntent(requireContext()))
            },
            onArticlesReadClick = { callback()?.onNavigateTo(NavTab.SEARCH) },
            onArticlesSavedClick = { callback()?.onNavigateTo(NavTab.READING_LISTS) },
            onExploreClick = {
                ActivityTabEvent.submit(activeInterface = "activity_tab", action = "explore_click", editCount = viewModel.getTotalEditsCount())
                callback()?.onNavigateTo(NavTab.READING_LISTS)
            },
            onCategoryItemClick = { category ->
                startActivity(CategoryActivity.newIntent(requireActivity(), viewModel.createPageTitleForCategory(category)))
            },
            onReadingHistoryRetry = { viewModel.loadReadingHistory() },
            onEditedPageClick = { pageTitle ->
                val entry = HistoryEntry(title = pageTitle, source = HistoryEntry.SOURCE_ACTIVITY_TAB)
                startActivity(PageActivity.newIntentForNewTab(context = requireActivity(), entry = entry, title = pageTitle))
            },
            onContributionsClick = {
                startActivity(UserContribListActivity.newIntent(requireActivity(), userName))
            },
            onSuggestedEditsClick = {
                ActivityTabEvent.submit(activeInterface = "activity_tab", action = "sugg_edit_click", editCount = viewModel.getTotalEditsCount())
                startActivity(SuggestedEditsTasksActivity.newIntent(requireActivity()))
            },
            onImpactRetry = { viewModel.loadImpact() },
            onPlayGameClick = {
                startActivity(OnThisDayGameActivity.newIntent(
                    context = requireContext(),
                    invokeSource = Constants.InvokeSource.ACTIVITY_TAB,
                    wikiSite = WikipediaApp.instance.wikiSite
                ))
            },
            onGameStatsClick = { startActivity(GamesHubActivity.newIntent(requireContext())) },
            onGamesRetry = { viewModel.loadWikiGamesStats() },
            onDonationClick = {
                ActivityTabEvent.submit(activeInterface = "activity_tab", action = "last_donation_click",
                    editCount = viewModel.getTotalEditsCount(), state = if (viewModel.hasNoDonationData()) "empty" else "complete")
                (requireActivity() as? BaseActivity)?.launchDonateDialog(campaignId = ActivityTabViewModel.CAMPAIGN_ID)
            },
            onTimelineItemClick = { handleTimelineItemClick(it) },
            yearInReviewEntryState = yearInReviewEntryState.takeIf { YearInReviewViewModel.canShowEntryPoint },
            onYirGetStartedClick = {
                // TODO: add yir announcement activity
            }
        )
    }

    companion object {
        fun newInstance(): ActivityTabFragment {
            return ActivityTabFragment().apply {
                arguments = Bundle().apply {
                    // TODO
                }
            }
        }
    }

    private fun handleMenuItemClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_customize_activity_tab -> {
                ActivityTabEvent.submit(activeInterface = "activity_tab_overflow_menu", action = "customize_click")
                startActivity(ActivityTabCustomizationActivity.newIntent(requireContext()))
                true
            }
            R.id.menu_clear_history -> {
                ActivityTabEvent.submit(activeInterface = "activity_tab_overflow_menu", action = "clear_history_click")
                HistoryFragment.clearAllHistory(requireContext(), lifecycleScope) {
                    viewModel.loadAll()
                }
                true
            }
            R.id.menu_clear_donation_history -> {
                ActivityTabEvent.submit(activeInterface = "activity_tab_overflow_menu", action = "clear_donation_history_click")
                Prefs.donationResults = emptyList()
                Prefs.activityTabModules = Prefs.activityTabModules.setModuleEnabled(ModuleType.DONATIONS, false)
                viewModel.loadAll()
                true
            }
            R.id.menu_learn_more -> {
                ActivityTabEvent.submit(activeInterface = "activity_tab_overflow_menu", action = "learn_click")
                UriUtil.visitInExternalBrowser(requireActivity(), getString(R.string.activity_tab_url).toUri())
                true
            }
            R.id.menu_report_feature -> {
                ActivityTabEvent.submit(activeInterface = "activity_tab_overflow_menu", action = "problem_click")
                FeedbackUtil.composeEmail(requireContext(),
                    subject = getString(R.string.activity_tab_report_email_subject),
                    body = getString(R.string.activity_tab_report_email_body))
                true
            }
            else -> false
        }
    }

    private fun handleTimelineItemClick(item: TimelineItem) {
        viewModel.shouldRefreshTimelineSilently = true
        when (item.activitySource) {
            ActivitySource.EDIT -> {
                startActivity(
                    ArticleEditDetailsActivity.newIntent(
                        requireContext(),
                        PageTitle(
                            item.apiTitle,
                            viewModel.wikiSiteForTimeline,
                            item.thumbnailUrl,
                            item.description,
                            item.displayTitle
                        ), item.pageId, revisionTo = item.id
                    )
                )
            } else -> {
                val pageTitle = item.toPageTitle()
                startActivity(
                    PageActivity.newIntentForCurrentTab(
                        requireContext(),
                        HistoryEntry(pageTitle, HistoryEntry.SOURCE_ACTIVITY_TAB),
                        pageTitle
                    )
                )
            }
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }
}

@Composable
fun CommonCardHeader(
    modifier: Modifier = Modifier,
    icon: Painter,
    title: String,
    subtitle: String? = null,
    useHtmlText: Boolean = false,
    showChevron: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = icon,
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
                if (useHtmlText) {
                    HtmlText(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                        color = WikipediaTheme.colors.primaryColor,
                        lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }
            }
            subtitle?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = WikipediaTheme.colors.secondaryColor
                )
            }
        }
        if (showChevron) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                tint = WikipediaTheme.colors.secondaryColor,
                contentDescription = null
            )
        }
    }
}

@Composable
fun ActivityTabShimmerView(
    size: Dp = 120.dp
) {
    val transition = rememberInfiniteTransition()
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(size = 12.dp))
            .fillMaxWidth()
            .shimmerEffect(transition = transition)
            .size(size)
    )
}
