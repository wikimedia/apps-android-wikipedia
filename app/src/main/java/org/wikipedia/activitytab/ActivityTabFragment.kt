package org.wikipedia.activitytab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.activitytab.timeline.ActivitySource
import org.wikipedia.activitytab.timeline.Timeline
import org.wikipedia.activitytab.timeline.TimelineDateSeparator
import org.wikipedia.activitytab.timeline.toHistoryEntry
import org.wikipedia.activitytab.timeline.toPageTitle
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.CategoryActivity
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.diff.ArticleEditDetailsActivity
import org.wikipedia.events.LoggedInEvent
import org.wikipedia.events.LoggedOutEvent
import org.wikipedia.events.LoggedOutInBackgroundEvent
import org.wikipedia.games.onthisday.OnThisDayGameActivity
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UiState
import java.time.LocalDateTime

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
        Prefs.activityTabRedDotShown = true

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                FlowEventBus.events.collectLatest { event ->
                    when (event) {
                        is LoggedInEvent, is LoggedOutEvent, is LoggedOutInBackgroundEvent -> viewModel.loadAll()
                    }
                }
            }
        }

        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ActivityTabScreen(
                        isLoggedIn = AccountUtil.isLoggedIn,
                        userName = AccountUtil.userName,
                        modules = Prefs.activityTabModules,
                        readingHistoryState = viewModel.readingHistoryState.collectAsState().value,
                        donationUiState = viewModel.donationUiState.collectAsState().value,
                        wikiGamesUiState = viewModel.wikiGamesUiState.collectAsState().value,
                        impactUiState = viewModel.impactUiState.collectAsState().value,
                        timelineFlow = viewModel.timelineFlow
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        viewModel.loadAll()
        requireActivity().invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        requireActivity().removeMenuProvider(menuProvider)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ActivityTabScreen(
        isLoggedIn: Boolean,
        userName: String,
        modules: ActivityTabModules,
        readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>,
        donationUiState: UiState<String?>,
        wikiGamesUiState: UiState<OnThisDayGameViewModel.GameStatistics?>,
        impactUiState: UiState<GrowthUserImpact>,
        timelineFlow: Flow<PagingData<TimelineDisplayItem>>
    ) {
        val timelineItems = timelineFlow.collectAsLazyPagingItems()
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.paperColor),
            containerColor = WikipediaTheme.colors.paperColor
        ) { paddingValues ->
            var isRefreshing by remember { mutableStateOf(false) }
            val state = rememberPullToRefreshState()
            if (readingHistoryState is UiState.Success) {
                isRefreshing = false
            }

            if (!isLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            modifier = Modifier.size(164.dp),
                            painter = painterResource(R.drawable.illustration_activity_tab_logged_out),
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = stringResource(R.string.activity_tab_logged_out_title),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            color = WikipediaTheme.colors.primaryColor
                        )
                        Button(
                            modifier = Modifier.padding(top = 16.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WikipediaTheme.colors.progressiveColor,
                                contentColor = WikipediaTheme.colors.paperColor,
                            ),
                            onClick = {
                                startActivity(
                                    LoginActivity.newIntent(
                                        requireContext(),
                                        LoginActivity.SOURCE_ACTIVITY
                                    )
                                )
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.ic_user_avatar),
                                tint = WikipediaTheme.colors.paperColor,
                                contentDescription = null
                            )
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.create_account_button),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            contentPadding = PaddingValues(horizontal = 18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WikipediaTheme.colors.paperColor,
                                contentColor = WikipediaTheme.colors.primaryColor,
                            ),
                            onClick = {
                                startActivity(
                                    LoginActivity.newIntent(
                                        requireContext(),
                                        LoginActivity.SOURCE_ACTIVITY,
                                        createAccountFirst = false
                                    )
                                )
                            },
                        ) {
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.menu_login),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                return@Scaffold
            }

            PullToRefreshBox(
                onRefresh = {
                    isRefreshing = true
                    timelineItems.refresh()
                    viewModel.loadAll()
                },
                isRefreshing = isRefreshing,
                state = state,
                indicator = {
                    Indicator(
                        state = state,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = WikipediaTheme.colors.paperColor,
                        color = WikipediaTheme.colors.progressiveColor
                    )
                }
            ) {
                LazyColumn {
                    if (modules.isModuleEnabled(ModuleType.TIME_SPENT) || modules.isModuleEnabled(ModuleType.READING_INSIGHTS)) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(paddingValues)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                WikipediaTheme.colors.paperColor,
                                                WikipediaTheme.colors.additionColor
                                            )
                                        )
                                    )
                            ) {
                                ReadingHistoryModule(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    userName = userName,
                                    showTimeSpent = modules.isModuleEnabled(ModuleType.TIME_SPENT),
                                    showInsights = modules.isModuleEnabled(ModuleType.READING_INSIGHTS),
                                    readingHistoryState = readingHistoryState,
                                    onArticlesReadClick = { callback()?.onNavigateTo(NavTab.SEARCH) },
                                    onArticlesSavedClick = { callback()?.onNavigateTo(NavTab.READING_LISTS) },
                                    onExploreClick = { callback()?.onNavigateTo(NavTab.READING_LISTS) },
                                    onCategoryItemClick = { category ->
                                        val pageTitle =
                                            viewModel.createPageTitleForCategory(category)
                                        startActivity(
                                            CategoryActivity.newIntent(
                                                requireActivity(),
                                                pageTitle
                                            )
                                        )
                                    },
                                    wikiErrorClickEvents = WikiErrorClickEvents(
                                        retryClickListener = {
                                            viewModel.loadReadingHistory()
                                        }
                                    )
                                )
                            }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(paddingValues)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            WikipediaTheme.colors.paperColor,
                                            WikipediaTheme.colors.additionColor
                                        )
                                    )
                                )
                        ) {
                            if (modules.isModuleEnabled(ModuleType.IMPACT)) {
                                // TODO: zomg do something with this!
                            }

                            if (modules.isModuleEnabled(ModuleType.GAMES) || modules.isModuleEnabled(ModuleType.DONATIONS)) {
                                Text(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                                    text = stringResource(R.string.activity_tab_highlights),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (modules.isModuleEnabled(ModuleType.GAMES)) {
                                WikiGamesModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    uiState = wikiGamesUiState,
                                    onEntryCardClick = {
                                        requireActivity().startActivity(OnThisDayGameActivity.newIntent(
                                            context = requireContext(),
                                            invokeSource = Constants.InvokeSource.ACTIVITY_TAB,
                                            wikiSite = WikipediaApp.instance.wikiSite
                                        ))
                                    },
                                    onStatsCardClick = {
                                        // TODO: link to the stats page when we have the WikiGames home page.
                                        requireActivity().startActivity(OnThisDayGameActivity.newIntent(
                                            context = requireContext(),
                                            invokeSource = Constants.InvokeSource.ACTIVITY_TAB,

                                            wikiSite = WikipediaApp.instance.wikiSite
                                        ))
                                    },
                                    wikiErrorClickEvents = WikiErrorClickEvents(
                                        retryClickListener = {
                                            viewModel.loadWikiGamesStats()
                                        }
                                    )
                                )
                            }

                            if (modules.isModuleEnabled(ModuleType.DONATIONS)) {
                                DonationModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    uiState = donationUiState,
                                    onClick = {
                                        (requireActivity() as? BaseActivity)?.launchDonateDialog(
                                            campaignId = ActivityTabViewModel.CAMPAIGN_ID
                                        )
                                    }
                                )
                            }

                            if (modules.isModuleEnabled(ModuleType.DONATIONS) || modules.isModuleEnabled(ModuleType.GAMES) || modules.isModuleEnabled(ModuleType.IMPACT)) {
                                // Add bottom padding only if at least one of the modules in this gradient box is enabled.
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (modules.isModuleEnabled(ModuleType.TIMELINE)) {
                        items(
                            count = timelineItems.itemCount,
                        ) { index ->
                            when (val displayItem = timelineItems[index]) {
                                is TimelineDisplayItem.DateSeparator -> {
                                    TimelineDateSeparator(
                                        date = displayItem.date,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 32.dp)
                                    )
                                }
                                is TimelineDisplayItem.TimelineEntry -> {
                                    Timeline(
                                        timelineItem = displayItem.item,
                                        onItemClick = { item ->
                                            when (item.activitySource) {
                                                ActivitySource.EDIT -> {
                                                    startActivity(
                                                        ArticleEditDetailsActivity.newIntent(
                                                            requireContext(),
                                                            PageTitle(
                                                                item.apiTitle,
                                                                viewModel.wikiSite
                                                            ), item.pageId, revisionTo = item.id
                                                        )
                                                    )
                                                }

                                                ActivitySource.BOOKMARKED -> {
                                                    val title = toPageTitle(item)
                                                    val entry = HistoryEntry(
                                                        title,
                                                        HistoryEntry.SOURCE_INTERNAL_LINK
                                                    )
                                                    startActivity(
                                                        PageActivity.newIntentForCurrentTab(
                                                            requireContext(),
                                                            entry,
                                                            entry.title
                                                        )
                                                    )
                                                }

                                                ActivitySource.LINK, ActivitySource.SEARCH -> {
                                                    val entry = toHistoryEntry(item)
                                                    startActivity(
                                                        PageActivity.newIntentForCurrentTab(
                                                            requireContext(),
                                                            entry,
                                                            entry.title
                                                        )
                                                    )
                                                }

                                                else -> {}
                                            }
                                        }
                                    )
                                }
                                null -> {}
                            }
                        }

                        when (timelineItems.loadState.append) {
                            LoadState.Loading -> {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = WikipediaTheme.colors.progressiveColor
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenPreview() {
        val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                isLoggedIn = true,
                userName = "User",
                modules = ActivityTabModules(isDonationsEnabled = true),
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 12345,
                    articlesReadThisMonth = 123,
                    lastArticleReadTime = LocalDateTime.now(),
                    articlesReadByWeek = listOf(0, 12, 34, 56),
                    articlesSavedThisMonth = 23,
                    lastArticleSavedTime = LocalDateTime.of(2025, 6, 1, 12, 30),
                    articlesSaved = listOf(
                        PageTitle(text = "Psychology of art", wiki = site, thumbUrl = "foo.jpg", description = "Study of mental functions and behaviors", displayText = null),
                        PageTitle(text = "Industrial design", wiki = site, thumbUrl = null, description = "Process of design applied to physical products", displayText = null),
                        PageTitle(text = "Dufourspitze", wiki = site, thumbUrl = "foo.jpg", description = "Highest mountain in Switzerland", displayText = null),
                        PageTitle(text = "Barack Obama", wiki = site, thumbUrl = "foo.jpg", description = "President of the United States from 2009 to 2017", displayText = null),
                        PageTitle(text = "Octagon house", wiki = site, thumbUrl = "foo.jpg", description = "North American house style briefly popular in the 1850s", displayText = null)
                    ),
                    topCategories = listOf(
                        Category(2025, 1, "Category:Ancient history", "en", 1),
                        Category(2025, 1, "Category:World literature", "en", 1),
                    )
                )),
                donationUiState = UiState.Success("5 days ago"),
                wikiGamesUiState = UiState.Success(OnThisDayGameViewModel.GameStatistics(
                    totalGamesPlayed = 10,
                    averageScore = 4.5,
                    currentStreak = 15,
                    bestStreak = 25
                )),
                impactUiState = UiState.Success(GrowthUserImpact(totalEditsCount = 12345)),
                timelineFlow = emptyFlow()
            )
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenEmptyPreview() {
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                isLoggedIn = true,
                userName = "User",
                modules = ActivityTabModules(isDonationsEnabled = true),
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 0,
                    articlesReadThisMonth = 0,
                    lastArticleReadTime = null,
                    articlesReadByWeek = listOf(0, 0, 0, 0),
                    articlesSavedThisMonth = 0,
                    lastArticleSavedTime = null,
                    articlesSaved = emptyList(),
                    topCategories = emptyList()
                )),
                donationUiState = UiState.Success("Unknown"),
                wikiGamesUiState = UiState.Success(null),
                impactUiState = UiState.Success(GrowthUserImpact()),
                timelineFlow = emptyFlow()
            )
        }
    }

    @Preview
    @Composable
    fun ActivityTabScreenLoggedOutPreview() {
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                isLoggedIn = false,
                userName = "User",
                modules = ActivityTabModules(),
                readingHistoryState = UiState.Success(ActivityTabViewModel.ReadingHistory(
                    timeSpentThisWeek = 0,
                    articlesReadThisMonth = 0,
                    lastArticleReadTime = null,
                    articlesReadByWeek = listOf(0, 0, 0, 0),
                    articlesSavedThisMonth = 0,
                    lastArticleSavedTime = null,
                    articlesSaved = emptyList(),
                    topCategories = emptyList()
                )),
                donationUiState = UiState.Success("Unknown"),
                wikiGamesUiState = UiState.Success(null),
                impactUiState = UiState.Success(GrowthUserImpact()),
                timelineFlow = emptyFlow()
            )
        }
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
                startActivity(ActivityTabCustomizationActivity.newIntent(requireContext()))
                true
            }
            R.id.menu_learn_more -> {
                // TODO: MARK_ACTIVITY_TAB --> add mediawiki page link
                true
            }
            R.id.menu_report_feature -> {
                FeedbackUtil.composeEmail(requireContext(),
                    subject = getString(R.string.activity_tab_report_email_subject),
                    body = getString(R.string.activity_tab_report_email_body))
                true
            }
            else -> false
        }
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }
}
