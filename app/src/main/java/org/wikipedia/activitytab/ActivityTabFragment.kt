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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import org.wikipedia.activitytab.timeline.TimelineDateSeparator
import org.wikipedia.activitytab.timeline.TimelineItem
import org.wikipedia.activitytab.timeline.TimelineModule
import org.wikipedia.activitytab.timeline.TimelineModuleEmptyView
import org.wikipedia.analytics.eventplatform.ActivityTabEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.CategoryActivity
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.extensions.shimmerEffect
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
import org.wikipedia.history.HistoryFragment
import org.wikipedia.login.LoginActivity
import org.wikipedia.navtab.NavTab
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.SuggestedEditsTasksActivity
import org.wikipedia.theme.Theme
import org.wikipedia.usercontrib.UserContribListActivity
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UiState
import org.wikipedia.util.UriUtil
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
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    ActivityTabScreen(
                        isLoggedIn = AccountUtil.isLoggedIn && !AccountUtil.isTemporaryAccount,
                        userName = AccountUtil.userName,
                        languageCode = WikipediaApp.instance.wikiSite.languageCode,
                        modules = Prefs.activityTabModules,
                        haveAtLeastOneDonation = Prefs.donationResults.isNotEmpty(),
                        areGamesAvailable = OnThisDayGameViewModel.isLangSupported(WikipediaApp.instance.wikiSite.languageCode),
                        refreshSilently = viewModel.shouldRefreshTimelineSilently,
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
        languageCode: String,
        modules: ActivityTabModules,
        haveAtLeastOneDonation: Boolean,
        areGamesAvailable: Boolean,
        refreshSilently: Boolean,
        readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>,
        donationUiState: UiState<String?>,
        wikiGamesUiState: UiState<OnThisDayGameViewModel.GameStatistics?>,
        impactUiState: UiState<Pair<GrowthUserImpact, Int>>,
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
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp)
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
                                contentColor = Color.White,
                            ),
                            onClick = {
                                ActivityTabEvent.submit(activeInterface = "activity_tab_login", action = "create_account_click")
                                startActivity(
                                    LoginActivity.newIntent(
                                        requireContext(),
                                        LoginActivity.SOURCE_ACTIVITY_TAB
                                    )
                                )
                            },
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.ic_user_avatar),
                                tint = Color.White,
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
                                ActivityTabEvent.submit(activeInterface = "activity_tab_login", action = "login_click")
                                startActivity(
                                    LoginActivity.newIntent(
                                        requireContext(),
                                        LoginActivity.SOURCE_ACTIVITY_TAB,
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

            if (modules.noModulesVisible(haveAtLeastOneDonation = haveAtLeastOneDonation, areGamesAvailable = areGamesAvailable)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            modifier = Modifier.size(164.dp),
                            painter = painterResource(R.drawable.illustration_activity_tab_empty),
                            contentDescription = null
                        )
                        HtmlText(
                            modifier = Modifier.padding(vertical = 16.dp),
                            text = stringResource(R.string.activity_tab_customize_screen_no_modules_message),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = WikipediaTheme.colors.primaryColor,
                            linkInteractionListener = {
                                startActivity(ActivityTabCustomizationActivity.newIntent(requireContext()))
                            }
                        )
                    }
                    return@Scaffold
                }
            }

            PullToRefreshBox(
                onRefresh = {
                    isRefreshing = true
                    viewModel.shouldRefreshTimelineSilently = false
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
                    if (modules.isModuleVisible(ModuleType.TIME_SPENT) || modules.isModuleVisible(ModuleType.READING_INSIGHTS)) {
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
                                    showTimeSpent = modules.isModuleVisible(ModuleType.TIME_SPENT),
                                    showInsights = modules.isModuleVisible(ModuleType.READING_INSIGHTS),
                                    readingHistoryState = readingHistoryState,
                                    onArticlesReadClick = { callback()?.onNavigateTo(NavTab.SEARCH) },
                                    onArticlesSavedClick = { callback()?.onNavigateTo(NavTab.READING_LISTS) },
                                    onExploreClick = {
                                        ActivityTabEvent.submit(activeInterface = "activity_tab", action = "explore_click", editCount = viewModel.getTotalEditsCount())
                                        callback()?.onNavigateTo(NavTab.READING_LISTS)
                                    },
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
                            if (modules.isModuleVisible(ModuleType.EDITING_INSIGHTS) || modules.isModuleVisible(ModuleType.IMPACT)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                                            .weight(1f),
                                        text = stringResource(R.string.activity_tab_impact),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = WikipediaTheme.colors.primaryColor
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                                            .align(Alignment.CenterVertically)
                                        .background(color = WikipediaTheme.colors.paperColor).border(
                                                1.5.dp,
                                                WikipediaTheme.colors.primaryColor,
                                                RoundedCornerShape(4.dp)
                                        )
                                    ) {
                                        Text(
                                            modifier = Modifier.padding(start = 4.dp, end = 4.5.dp, top = 3.5.dp, bottom = 3.dp),
                                            text = languageCode.uppercase(),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = WikipediaTheme.colors.primaryColor
                                        )
                                    }
                                }
                            }

                            if (modules.isModuleVisible(ModuleType.EDITING_INSIGHTS)) {
                                EditingInsightsModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    uiState = impactUiState,
                                    onPageItemClick = {
                                        val entry = HistoryEntry(
                                            title = it,
                                            source = HistoryEntry.SOURCE_ACTIVITY_TAB
                                        )
                                        requireActivity().startActivity(
                                            PageActivity.newIntentForNewTab(
                                            context = requireActivity(),
                                            entry = entry,
                                            title = it
                                        ))
                                    },
                                    onContributionClick = {
                                        requireActivity().startActivity(
                                            UserContribListActivity.newIntent(
                                            context = requireActivity(),
                                            userName = userName
                                        ))
                                    },
                                    onSuggestedEditsClick = {
                                        ActivityTabEvent.submit(activeInterface = "activity_tab", action = "sugg_edit_click", editCount = viewModel.getTotalEditsCount())
                                        requireActivity().startActivity(
                                            SuggestedEditsTasksActivity.newIntent(
                                            context = requireActivity()
                                        ))
                                    },
                                    wikiErrorClickEvents = WikiErrorClickEvents(
                                        retryClickListener = {
                                            viewModel.loadImpact()
                                        }
                                    )
                                )
                            }

                            if (modules.isModuleVisible(ModuleType.IMPACT)) {
                                ImpactModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    uiState = impactUiState,
                                    onTotalEditsClick = {
                                        startActivity(UserContribListActivity.newIntent(requireContext(), userName))
                                    },
                                    wikiErrorClickEvents = WikiErrorClickEvents(
                                        retryClickListener = {
                                            viewModel.loadImpact()
                                        }
                                    )
                                )
                            }

                            if (modules.isModuleVisible(ModuleType.GAMES, areGamesAvailable = areGamesAvailable) || modules.isModuleVisible(ModuleType.DONATIONS)) {
                                Text(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                                    text = stringResource(R.string.activity_tab_highlights),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = WikipediaTheme.colors.primaryColor
                                )
                            }

                            if (modules.isModuleVisible(ModuleType.GAMES, areGamesAvailable = areGamesAvailable)) {
                                WikiGamesModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    uiState = wikiGamesUiState,
                                    onPlayGameCardClick = {
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

                            if (modules.isModuleVisible(ModuleType.DONATIONS, haveAtLeastOneDonation = haveAtLeastOneDonation)) {
                                DonationModule(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                    uiState = donationUiState,
                                    onClick = {
                                        ActivityTabEvent.submit(activeInterface = "activity_tab", action = "last_donation_click",
                                            editCount = viewModel.getTotalEditsCount(), state = if (viewModel.hasNoDonationData()) "empty" else "complete")
                                        (requireActivity() as? BaseActivity)?.launchDonateDialog(
                                            campaignId = ActivityTabViewModel.CAMPAIGN_ID
                                        )
                                    }
                                )
                            }

                            if (modules.isModuleVisible(ModuleType.DONATIONS, haveAtLeastOneDonation = haveAtLeastOneDonation) ||
                                modules.isModuleVisible(ModuleType.GAMES, areGamesAvailable = areGamesAvailable) ||
                                modules.isModuleVisible(ModuleType.EDITING_INSIGHTS) ||
                                modules.isModuleEnabled(ModuleType.IMPACT)) {
                                // Add bottom padding only if at least one of the modules in this gradient box is enabled.
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (modules.isModuleVisible(ModuleType.TIMELINE)) {
                        val isRefreshing = timelineItems.loadState.refresh is LoadState.Loading
                        val isEmpty = timelineItems.itemCount == 0
                        when {
                            // Show loading for fresh navigation or explicit refresh, User came from tab navigation OR pulled to refresh
                            isRefreshing && !refreshSilently -> {
                                item {
                                    ActivityTabShimmerView()
                                }
                                return@LazyColumn
                            }
                            // Show loading UI during silent refresh transition
                            // User clicked timeline item (shouldRefreshTimelineSilently = true) and returned,
                            // but timeline data is still loading/empty. Without this case, user would see empty state briefly before data loads instead of loading UI.
                            isEmpty && refreshSilently -> {
                                item {
                                    ActivityTabShimmerView()
                                }
                                return@LazyColumn
                            }
                            // empty timeline - no data available
                            isEmpty -> {
                                item {
                                    TimelineModuleEmptyView(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 32.dp, bottom = 52.dp)
                                    )
                                }
                                return@LazyColumn
                            }
                        }

                        items(
                            count = timelineItems.itemCount,
                        ) { index ->
                            when (val displayItem = timelineItems[index]) {
                                is TimelineDisplayItem.DateSeparator -> {
                                    TimelineDateSeparator(
                                        date = displayItem.date,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(top = 22.dp, bottom = 8.dp)
                                    )
                                }
                                is TimelineDisplayItem.TimelineEntry -> {
                                    TimelineModule(
                                        timelineItem = displayItem.item,
                                        onItemClick = {
                                            handleTimelineItemClick(it)
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                                null -> {}
                            }
                        }

                        if (timelineItems.loadState.append is LoadState.Loading) {
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
                languageCode = "en",
                modules = ActivityTabModules(isDonationsEnabled = true),
                haveAtLeastOneDonation = true,
                areGamesAvailable = true,
                refreshSilently = false,
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
                impactUiState = UiState.Success(Pair(GrowthUserImpact(totalEditsCount = 12345), 123456)),
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
                languageCode = "ru",
                modules = ActivityTabModules(isDonationsEnabled = true),
                haveAtLeastOneDonation = false,
                areGamesAvailable = false,
                refreshSilently = false,
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
                impactUiState = UiState.Success(Pair(GrowthUserImpact(), 0)),
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
                languageCode = "he",
                modules = ActivityTabModules(),
                haveAtLeastOneDonation = false,
                areGamesAvailable = false,
                refreshSilently = false,
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
                impactUiState = UiState.Success(Pair(GrowthUserImpact(), 0)),
                timelineFlow = emptyFlow()
            )
        }
    }

    @Preview
    @Composable
    fun ActivityTabNoModulesPreview() {
        BaseTheme(currentTheme = Theme.LIGHT) {
            ActivityTabScreen(
                isLoggedIn = true,
                userName = "User",
                languageCode = "zh",
                modules = ActivityTabModules(
                    isTimeSpentEnabled = false,
                    isReadingInsightsEnabled = false,
                    isEditingInsightsEnabled = false,
                    isImpactEnabled = false,
                    isGamesEnabled = false,
                    isDonationsEnabled = false,
                    isTimelineEnabled = false
                ),
                haveAtLeastOneDonation = true,
                areGamesAvailable = true,
                refreshSilently = false,
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
                impactUiState = UiState.Success(Pair(GrowthUserImpact(), 0)),
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
fun ActivityTabShimmerView(
    size: Dp = 120.dp
) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(size = 12.dp))
            .fillMaxWidth()
            .shimmerEffect()
            .size(size)
    )
}
