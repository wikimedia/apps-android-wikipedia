package org.wikipedia.activitytab

import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import org.wikipedia.R
import org.wikipedia.activitytab.timeline.TimelineDateSeparator
import org.wikipedia.activitytab.timeline.TimelineItem
import org.wikipedia.activitytab.timeline.TimelineModule
import org.wikipedia.activitytab.timeline.TimelineModuleEmptyView
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiLangCodeBox
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import org.wikipedia.yearinreview.presentation.YearInReviewEntryCard
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTabLoggedInScreen(
    userName: String,
    languageCode: String,
    modules: ActivityTabModules,
    haveAtLeastOneDonation: Boolean,
    areGamesAvailable: Boolean,
    refreshSilently: Boolean,
    scrollToGames: Boolean = false,
    readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>,
    donationUiState: UiState<String?>,
    wikiGamesUiState: UiState<OnThisDayGameViewModel.GameStatistics?>,
    impactUiState: UiState<Pair<GrowthUserImpact, Int>>,
    timelineFlow: Flow<PagingData<TimelineDisplayItem>>,
    onScrollToGamesConsumed: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onCustomizeClick: () -> Unit = {},
    onArticlesReadClick: () -> Unit = {},
    onArticlesSavedClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onCategoryItemClick: (Category) -> Unit = {},
    onReadingHistoryRetry: () -> Unit = {},
    onEditedPageClick: (PageTitle) -> Unit = {},
    onContributionsClick: () -> Unit = {},
    onSuggestedEditsClick: () -> Unit = {},
    onImpactRetry: () -> Unit = {},
    onPlayGameClick: () -> Unit = {},
    onGameStatsClick: () -> Unit = {},
    onGamesRetry: () -> Unit = {},
    onDonationClick: () -> Unit = {},
    onTimelineItemClick: (TimelineItem) -> Unit = {},
    yearInReviewEntryState: UiState<Boolean>? = null,
    onYirGetStartedClick: () -> Unit = {}
) {
    val showYearInReviewEntry = yearInReviewEntryState is UiState.Loading || yearInReviewEntryState is UiState.Success
    val timelineItems = timelineFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    var gamesModuleOffsetInItem by remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollToGames) {
        if (scrollToGames && modules.isModuleVisible(ModuleType.GAMES, areGamesAvailable = areGamesAvailable)) {
            val yearInReviewItemCount = if (showYearInReviewEntry) 1 else 0
            val readingHistoryItemCount = if (
                modules.isModuleVisible(ModuleType.TIME_SPENT) ||
                modules.isModuleVisible(ModuleType.READING_INSIGHTS)
            ) 1 else 0
            val containerIndex = yearInReviewItemCount + readingHistoryItemCount

            gamesModuleOffsetInItem = 0

            // since we don't have index per module, this will move to the container holding games module
            // so that the lazy column can compose it and onGloballyPositioned executes
            listState.scrollToItem(containerIndex)

            // now we wait for the games module to be laid out
            snapshotFlow { gamesModuleOffsetInItem }
                .first { it > 0 }

            // then animate to the correct offset
            listState.animateScrollToItem(
                index = containerIndex,
                scrollOffset = gamesModuleOffsetInItem
            )
            onScrollToGamesConsumed()
        }
    }

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

        if (modules.noModulesVisible(haveAtLeastOneDonation = haveAtLeastOneDonation, areGamesAvailable = areGamesAvailable)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (showYearInReviewEntry) {
                    LoggedInYearInReviewEntry(
                        state = yearInReviewEntryState,
                        onGetStartedClick = onYirGetStartedClick
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
                            linkInteractionListener = { onCustomizeClick() }
                        )
                    }
                }
                return@Scaffold
            }
        }

        PullToRefreshBox(
            onRefresh = {
                isRefreshing = true
                timelineItems.refresh()
                onRefresh()
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
            LazyColumn(
                state = listState
            ) {
                if (showYearInReviewEntry) {
                    item {
                        LoggedInYearInReviewEntry(
                            modifier = Modifier.padding(paddingValues),
                            state = yearInReviewEntryState,
                            onGetStartedClick = onYirGetStartedClick
                        )
                    }
                }
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
                                onArticlesReadClick = onArticlesReadClick,
                                onArticlesSavedClick = onArticlesSavedClick,
                                onExploreClick = onExploreClick,
                                onCategoryItemClick = onCategoryItemClick,
                                wikiErrorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = onReadingHistoryRetry
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
                                modifier = Modifier
                                    .padding(start = 16.dp, end = 16.dp, top = 24.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    modifier = Modifier
                                        .weight(1f),
                                    text = stringResource(R.string.activity_tab_impact),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = WikipediaTheme.colors.primaryColor
                                )
                                WikiLangCodeBox(
                                    modifier = Modifier
                                        .height(20.dp)
                                        .widthIn(min = 20.dp),
                                    languageCode = languageCode
                                )
                            }
                        }

                        if (modules.isModuleVisible(ModuleType.EDITING_INSIGHTS)) {
                            EditingInsightsModule(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                uiState = impactUiState,
                                onPageItemClick = onEditedPageClick,
                                onContributionClick = onContributionsClick,
                                onSuggestedEditsClick = onSuggestedEditsClick,
                                wikiErrorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = onImpactRetry
                                )
                            )
                        }

                        if (modules.isModuleVisible(ModuleType.IMPACT)) {
                            ImpactModule(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                uiState = impactUiState,
                                onTotalEditsClick = onContributionsClick,
                                wikiErrorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = onImpactRetry
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
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                                    .onGloballyPositioned { coordinates ->
                                        val offset = coordinates.positionInParent().y.toInt()
                                        gamesModuleOffsetInItem = offset
                                    },
                                uiState = wikiGamesUiState,
                                onPlayGameCardClick = onPlayGameClick,
                                onStatsCardClick = onGameStatsClick,
                                wikiErrorClickEvents = WikiErrorClickEvents(
                                    retryClickListener = onGamesRetry
                                )
                            )
                        }

                        if (modules.isModuleVisible(ModuleType.DONATIONS, haveAtLeastOneDonation = haveAtLeastOneDonation)) {
                            DonationModule(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                                uiState = donationUiState,
                                onClick = onDonationClick
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
                    val isTimelineRefreshing = timelineItems.loadState.refresh is LoadState.Loading
                    val isEmpty = timelineItems.itemCount == 0
                    when {
                        // Show loading for fresh navigation or explicit refresh, User came from tab navigation OR pulled to refresh
                        isTimelineRefreshing && !refreshSilently -> {
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
                                    onItemClick = onTimelineItemClick
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

@Composable
private fun LoggedInYearInReviewEntry(
    state: UiState<Boolean>?,
    modifier: Modifier = Modifier,
    onGetStartedClick: () -> Unit
) {
    val entryModifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = entryModifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect(transition = rememberInfiniteTransition())
            )
        }
        is UiState.Success -> {
            YearInReviewEntryCard(
                modifier = entryModifier,
                title = stringResource(if (state.data) R.string.year_in_review_entry_card_title_personalized else R.string.year_in_review_entry_card_title_collective),
                subtitle = stringResource(R.string.year_in_review_entry_card_subtitle),
                onGetStartedClick = onGetStartedClick
            )
        }
        else -> {}
    }
}

private val emptyReadingHistory = ActivityTabViewModel.ReadingHistory(
    timeSpentThisWeek = 0,
    articlesReadThisMonth = 0,
    lastArticleReadTime = null,
    articlesReadByWeek = listOf(0, 0, 0, 0),
    articlesSavedThisMonth = 0,
    lastArticleSavedTime = null,
    articlesSaved = emptyList(),
    topCategories = emptyList()
)

@Preview
@Composable
private fun ActivityTabLoggedInScreenPreview() {
    val site = WikiSite("https://en.wikipedia.org/".toUri(), "en")
    BaseTheme(currentTheme = Theme.LIGHT) {
        ActivityTabLoggedInScreen(
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
            timelineFlow = emptyFlow(),
            yearInReviewEntryState = UiState.Success(true)
        )
    }
}

@Preview
@Composable
private fun ActivityTabLoggedInScreenEmptyPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ActivityTabLoggedInScreen(
            userName = "User",
            languageCode = "ru",
            modules = ActivityTabModules(isDonationsEnabled = true),
            haveAtLeastOneDonation = false,
            areGamesAvailable = false,
            refreshSilently = false,
            readingHistoryState = UiState.Success(emptyReadingHistory),
            donationUiState = UiState.Success("Unknown"),
            wikiGamesUiState = UiState.Success(null),
            impactUiState = UiState.Success(Pair(GrowthUserImpact(), 0)),
            timelineFlow = emptyFlow()
        )
    }
}

@Preview
@Composable
private fun ActivityTabLoggedInScreenNoModulesPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ActivityTabLoggedInScreen(
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
            readingHistoryState = UiState.Success(emptyReadingHistory),
            donationUiState = UiState.Success("Unknown"),
            wikiGamesUiState = UiState.Success(null),
            impactUiState = UiState.Success(Pair(GrowthUserImpact(), 0)),
            timelineFlow = emptyFlow(),
            yearInReviewEntryState = UiState.Loading
        )
    }
}
