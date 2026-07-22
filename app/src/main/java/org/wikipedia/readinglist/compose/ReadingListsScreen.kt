package org.wikipedia.readinglist.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.MessageCard
import org.wikipedia.compose.components.SearchEmptyView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.readinglist.OnboardingState
import org.wikipedia.readinglist.ReadingListRow
import org.wikipedia.readinglist.ReadingListUiModel
import org.wikipedia.readinglist.ReadingListsUiState
import org.wikipedia.readinglist.RecommendedReadingListCard
import org.wikipedia.readinglist.RecommendedReadingListDiscoverCardView
import org.wikipedia.readinglist.SavedTab
import org.wikipedia.readinglist.recommended.RecommendedReadingListUpdateFrequency
import org.wikipedia.theme.Theme
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

@Composable
fun ReadingListsScreen(
    uiState: ReadingListsUiState,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    pullToRefreshEnabled: Boolean = true,
    isSelectionMode: Boolean = false,
    selectedListIds: Set<Long> = emptySet(),
    selectedPageIds: Set<Long> = emptySet(),
    selectedTab: SavedTab = SavedTab.ALL_ARTICLES,
    showCollectionsBadge: Boolean = false,
    onSelectTab: (SavedTab) -> Unit = {},
    onOnboardingAction: (OnboardingAction) -> Unit = {},
    onRefresh: () -> Unit = {},
    onListClick: (Long) -> Unit = {},
    onListMenuAction: (Long, ReadingListMenuAction) -> Unit = { _, _ -> },
    onListSelectionChange: (Long) -> Unit = {},
    onPageSelectionChange: (Long) -> Unit = {},
    onPageClick: (Long) -> Unit = {},
    onPageLongClick: (Long) -> Unit = {},
    onPageChipClick: (Long) -> Unit = {},
    onPageToggleOfflineClick: (Long) -> Unit = {},
    onDiscoverCardClick: () -> Unit = {}
) {
    LaunchedEffect(uiState.onboarding) {
        if (uiState.onboarding == OnboardingState.RecommendedReadingList) {
            onOnboardingAction(OnboardingAction.RecommendedShown)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (!uiState.isSearchActive && !isSelectionMode) {
            SavedTabBar(
                selectedTab = selectedTab,
                showCollectionsBadge = showCollectionsBadge,
                onSelectTab = onSelectTab
            )
        }

        if (pullToRefreshEnabled) {
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
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
                ReadingListsContent(
                    uiState = uiState,
                    isSelectionMode = isSelectionMode,
                    selectedListIds = selectedListIds,
                    selectedPageIds = selectedPageIds,
                    onOnboardingAction = onOnboardingAction,
                    onListClick = onListClick,
                    onListMenuAction = onListMenuAction,
                    onListSelectionChange = onListSelectionChange,
                    onPageSelectionChange = onPageSelectionChange,
                    onPageClick = onPageClick,
                    onPageLongClick = onPageLongClick,
                    onPageChipClick = onPageChipClick,
                    onPageToggleOfflineClick = onPageToggleOfflineClick,
                    onDiscoverCardClick = onDiscoverCardClick
                )
            }
        } else {
            ReadingListsContent(
                uiState = uiState,
                isSelectionMode = isSelectionMode,
                selectedListIds = selectedListIds,
                selectedPageIds = selectedPageIds,
                onOnboardingAction = onOnboardingAction,
                onListClick = onListClick,
                onListMenuAction = onListMenuAction,
                onListSelectionChange = onListSelectionChange,
                onPageSelectionChange = onPageSelectionChange,
                onPageClick = onPageClick,
                onPageLongClick = onPageLongClick,
                onPageChipClick = onPageChipClick,
                onPageToggleOfflineClick = onPageToggleOfflineClick,
                onDiscoverCardClick = onDiscoverCardClick
            )
        }
    }
}

@Composable
private fun SavedTabBar(
    selectedTab: SavedTab,
    modifier: Modifier = Modifier,
    showCollectionsBadge: Boolean = false,
    onSelectTab: (SavedTab) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SavedTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val label = when (tab) {
                    SavedTab.ALL_ARTICLES -> stringResource(R.string.reading_lists_tab_all_articles)
                    SavedTab.COLLECTIONS -> stringResource(R.string.reading_lists_tab_collections)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(tab) }
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Width wraps the label so the underline matches the text width, not the whole tab.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(IntrinsicSize.Max)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                color = if (isSelected) {
                                    WikipediaTheme.colors.progressiveColor
                                } else {
                                    WikipediaTheme.colors.primaryColor
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (tab == SavedTab.COLLECTIONS && showCollectionsBadge) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(WikipediaTheme.colors.destructiveColor)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(
                                    if (isSelected) WikipediaTheme.colors.progressiveColor else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            color = WikipediaTheme.colors.borderColor,
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun ReadingListsContent(
    uiState: ReadingListsUiState,
    isSelectionMode: Boolean,
    selectedListIds: Set<Long>,
    selectedPageIds: Set<Long>,
    onOnboardingAction: (OnboardingAction) -> Unit,
    onListClick: (Long) -> Unit,
    onListMenuAction: (Long, ReadingListMenuAction) -> Unit,
    onListSelectionChange: (Long) -> Unit,
    onPageSelectionChange: (Long) -> Unit,
    onPageClick: (Long) -> Unit,
    onPageLongClick: (Long) -> Unit,
    onPageChipClick: (Long) -> Unit,
    onPageToggleOfflineClick: (Long) -> Unit,
    onDiscoverCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
        }
        uiState.rows.isEmpty() && uiState.searchQuery.isNullOrEmpty() &&
            uiState.onboarding == OnboardingState.None && uiState.discoverCard == null -> {
            EmptyReadingLists(modifier = modifier)
        }
        uiState.rows.isEmpty() && !uiState.searchQuery.isNullOrEmpty() -> {
            SearchEmptyView(
                modifier = modifier.fillMaxSize(),
                emptyTexTitle = stringResource(R.string.search_reading_lists_no_results)
            )
        }
        else -> {
            ReadingListsList(
                rows = uiState.rows,
                pageDownloadProgress = uiState.pageDownloadProgress,
                sortMode = uiState.sortMode,
                onboarding = uiState.onboarding,
                discoverCard = uiState.discoverCard,
                onOnboardingAction = onOnboardingAction,
                isSelectionMode = isSelectionMode,
                selectedListIds = selectedListIds,
                selectedPageIds = selectedPageIds,
                onListClick = onListClick,
                onListMenuAction = onListMenuAction,
                onListSelectionChange = onListSelectionChange,
                onPageSelectionChange = onPageSelectionChange,
                onPageClick = onPageClick,
                onPageLongClick = onPageLongClick,
                onPageChipClick = onPageChipClick,
                onPageToggleOfflineClick = onPageToggleOfflineClick,
                onDiscoverCardClick = onDiscoverCardClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun OnboardingCard(
    state: OnboardingState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)

    when (state) {
        OnboardingState.RecommendedReadingList -> {
            MessageCard(
                modifier = cardModifier,
                label = stringResource(R.string.recommended_reading_list_onboarding_card_new),
                title = stringResource(R.string.recommended_reading_list_onboarding_card_title),
                message = stringResource(R.string.recommended_reading_list_onboarding_card_message),
                positiveButtonText = stringResource(R.string.recommended_reading_list_onboarding_card_positive_button),
                onPositiveButtonClick = { onAction(OnboardingAction.RecommendedAccept) },
                onContainerClick = { onAction(OnboardingAction.RecommendedAccept) },
                negativeButtonText = stringResource(R.string.recommended_reading_list_onboarding_card_negative_button),
                onNegativeButtonClick = { onAction(OnboardingAction.RecommendedDismiss) }
            )
        }
        OnboardingState.SyncReminder -> {
            MessageCard(
                modifier = cardModifier,
                title = stringResource(R.string.reading_lists_sync_reminder_title),
                message = StringUtil.fromHtml(stringResource(R.string.reading_lists_sync_reminder_text)).toString(),
                imageRes = ResourceUtil.getThemedAttributeId(context, R.attr.sync_reading_list_prompt_drawable),
                positiveButtonText = stringResource(R.string.reading_lists_sync_reminder_action),
                onPositiveButtonClick = { onAction(OnboardingAction.SyncEnable) },
                onContainerClick = { onAction(OnboardingAction.SyncEnable) },
                negativeButtonText = stringResource(R.string.reading_lists_ignore_button),
                onNegativeButtonClick = { onAction(OnboardingAction.SyncDismiss) }
            )
        }
        OnboardingState.LoginReminder -> {
            MessageCard(
                modifier = cardModifier,
                title = stringResource(R.string.reading_list_login_reminder_title),
                message = stringResource(R.string.reading_lists_login_reminder_text),
                imageRes = ResourceUtil.getThemedAttributeId(context, R.attr.sync_reading_list_prompt_drawable),
                positiveButtonText = stringResource(R.string.reading_lists_login_button),
                onPositiveButtonClick = { onAction(OnboardingAction.LoginRequest) },
                onContainerClick = { onAction(OnboardingAction.LoginRequest) },
                negativeButtonText = stringResource(R.string.reading_lists_ignore_button),
                onNegativeButtonClick = { onAction(OnboardingAction.LoginDismiss) }
            )
        }
        OnboardingState.None -> Unit
    }
}

@Composable
private fun DiscoverCard(
    card: RecommendedReadingListCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = if (card.isUserLoggedIn) {
        stringResource(R.string.recommended_reading_list_page_subtitle_made_for, "<b>${card.userName}</b>")
    } else {
        stringResource(R.string.recommended_reading_list_page_logged_out_subtitle_made_for_you)
    }
    val description = stringResource(
        when (card.updateFrequency) {
            RecommendedReadingListUpdateFrequency.DAILY -> R.string.recommended_reading_list_page_description_daily
            RecommendedReadingListUpdateFrequency.WEEKLY -> R.string.recommended_reading_list_page_description_weekly
            RecommendedReadingListUpdateFrequency.MONTHLY -> R.string.recommended_reading_list_page_description_monthly
        }
    )
    Box(modifier = modifier
        .padding(16.dp)
    ) {
        RecommendedReadingListDiscoverCardView(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            title = stringResource(R.string.recommended_reading_list_title),
            subtitleIcon = R.drawable.ic_wikipedia_w,
            subtitle = subtitle,
            description = description,
            images = card.images,
            isNewListGenerated = card.isNewListGenerated,
            isUserLoggedIn = card.isUserLoggedIn
        )
    }
}

sealed interface OnboardingAction {
    data object RecommendedShown : OnboardingAction
    data object RecommendedAccept : OnboardingAction
    data object RecommendedDismiss : OnboardingAction
    data object SyncEnable : OnboardingAction
    data object SyncDismiss : OnboardingAction
    data object LoginRequest : OnboardingAction
    data object LoginDismiss : OnboardingAction
}

@Composable
private fun ReadingListsList(
    rows: List<ReadingListRow>,
    pageDownloadProgress: Map<Long, Int>,
    sortMode: Int,
    onboarding: OnboardingState,
    discoverCard: RecommendedReadingListCard?,
    onOnboardingAction: (OnboardingAction) -> Unit,
    isSelectionMode: Boolean,
    selectedListIds: Set<Long>,
    selectedPageIds: Set<Long>,
    onListClick: (Long) -> Unit,
    onListMenuAction: (Long, ReadingListMenuAction) -> Unit,
    onListSelectionChange: (Long) -> Unit,
    onPageSelectionChange: (Long) -> Unit,
    onPageClick: (Long) -> Unit,
    onPageLongClick: (Long) -> Unit,
    onPageChipClick: (Long) -> Unit,
    onPageToggleOfflineClick: (Long) -> Unit,
    onDiscoverCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(onboarding, discoverCard) {
        if (onboarding != OnboardingState.None || discoverCard != null) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState
    ) {
        if (onboarding != OnboardingState.None) {
            item(key = onboarding.toString()) {
                OnboardingCard(
                    state = onboarding,
                    onAction = onOnboardingAction
                )
            }
        }
        if (discoverCard != null) {
            item(key = "discover") {
                DiscoverCard(
                    card = discoverCard,
                    onClick = onDiscoverCardClick
                )
            }
        }
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is ReadingListRow.ListRow -> "list-$sortMode-${row.list.id}"
                    is ReadingListRow.PageRow -> "page-$sortMode-${row.page.id}"
                }
            }
        ) { row ->
            when (row) {
                is ReadingListRow.ListRow -> ReadingListRow(
                    list = row.list,
                    isSelectionMode = isSelectionMode,
                    isSelected = row.list.id in selectedListIds,
                    onSelectionChange = { onListSelectionChange(row.list.id) },
                    onClick = { onListClick(row.list.id) },
                    onMenuAction = { action -> onListMenuAction(row.list.id, action) }
                )
                is ReadingListRow.PageRow -> ReadingListPageRow(
                    page = row.page,
                    isSelectionMode = isSelectionMode,
                    isSelected = row.page.id in selectedPageIds,
                    downloadProgress = pageDownloadProgress[row.page.id] ?: 0,
                    containingLists = row.containingLists,
                    onSelectionChange = { onPageSelectionChange(row.page.id) },
                    onClick = { onPageClick(row.page.id) },
                    onLongClick = { onPageLongClick(row.page.id) },
                    onToggleOfflineClick = { onPageToggleOfflineClick(row.page.id) },
                    onChipClick = { listId -> onPageChipClick(listId) }
                )
            }
            HorizontalDivider(
                color = WikipediaTheme.colors.borderColor,
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun EmptyReadingLists(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.saved_list_empty_title),
            color = WikipediaTheme.colors.primaryColor,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.reading_lists_empty_message),
            color = WikipediaTheme.colors.secondaryColor,
            style = MaterialTheme.typography.bodyLarge.copy(
                letterSpacing = 0.15.sp,
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Preview
@Composable
private fun ReadingListsScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListsScreen(
            selectedTab = SavedTab.COLLECTIONS,
            showCollectionsBadge = true,
            uiState = ReadingListsUiState(
                isLoading = false,
                rows = listOf(
                    ReadingListRow.ListRow(
                        ReadingListUiModel(
                            id = 1,
                            title = "Default",
                            description = null,
                            isDefault = true,
                            totalPages = 3,
                            sizeBytesFromPages = 0
                        )
                    ),
                    ReadingListRow.ListRow(
                        ReadingListUiModel(
                            id = 2,
                            title = "Physics",
                            description = "reading",
                            isDefault = false,
                            totalPages = 12,
                            sizeBytesFromPages = 1240000
                        )
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ReadingListsScreenAllArticlesTabPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListsScreen(
            selectedTab = SavedTab.ALL_ARTICLES,
            showCollectionsBadge = true,
            uiState = ReadingListsUiState(
                isLoading = false,
                rows = listOf(
                    ReadingListRow.ListRow(
                        ReadingListUiModel(
                            id = 2,
                            title = "Physics",
                            description = "reading",
                            isDefault = false,
                            totalPages = 12,
                            sizeBytesFromPages = 1240000
                        )
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ReadingListsEmptyPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadingListsScreen(
            uiState = ReadingListsUiState(isLoading = false)
        )
    }
}

@Preview
@Composable
private fun ReadingListsOnboardingPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadingListsScreen(
            selectedTab = SavedTab.COLLECTIONS,
            uiState = ReadingListsUiState(
                isLoading = false,
                rows = listOf(
                    ReadingListRow.ListRow(
                        ReadingListUiModel(
                            id = 2,
                            title = "Physics",
                            description = "reading",
                            isDefault = false,
                            totalPages = 12,
                            sizeBytesFromPages = 1240000
                        )
                    )
                ),
                onboarding = OnboardingState.RecommendedReadingList
            )
        )
    }
}

@Preview
@Composable
private fun ReadingListsDiscoverCardPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadingListsScreen(
            selectedTab = SavedTab.COLLECTIONS,
            showCollectionsBadge = true,
            uiState = ReadingListsUiState(
                isLoading = false,
                rows = listOf(
                    ReadingListRow.ListRow(
                        ReadingListUiModel(
                            id = 2,
                            title = "Physics",
                            description = "reading",
                            isDefault = false,
                            totalPages = 12,
                            sizeBytesFromPages = 1240000
                        )
                    )
                ),
                discoverCard = RecommendedReadingListCard(
                    images = emptyList(),
                    isNewListGenerated = true,
                    isUserLoggedIn = true,
                    userName = "Wikipedian",
                    updateFrequency = RecommendedReadingListUpdateFrequency.WEEKLY
                )
            )
        )
    }
}
