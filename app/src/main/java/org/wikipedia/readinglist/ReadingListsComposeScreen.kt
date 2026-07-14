package org.wikipedia.readinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.wikipedia.readinglist.compose.ReadingListMenuAction
import org.wikipedia.readinglist.compose.ReadingListPageRow
import org.wikipedia.readinglist.compose.ReadingListRow
import org.wikipedia.theme.Theme
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

@Composable
fun ReadingListsComposeScreen(
    uiState: ReadingListsUiState,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    pullToRefreshEnabled: Boolean = true,
    isSelectionMode: Boolean = false,
    selectedListIds: Set<Long> = emptySet(),
    onOnboardingAction: (OnboardingAction) -> Unit = {},
    onRefresh: () -> Unit = {},
    onListClick: (Long) -> Unit = {},
    onListMenuAction: (Long, ReadingListMenuAction) -> Unit = { _, _ -> },
    onListSelectionChange: (Long) -> Unit = {},
    onPageClick: (Long) -> Unit = {},
    onPageLongClick: (Long) -> Unit = {},
    onPageChipClick: (Long) -> Unit = {}
) {
    LaunchedEffect(uiState.onboarding) {
        if (uiState.onboarding == OnboardingState.RecommendedReadingList) {
            onOnboardingAction(OnboardingAction.RecommendedShown)
        }
    }

    if (pullToRefreshEnabled) {
        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            modifier = modifier.fillMaxSize(),
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
                onOnboardingAction = onOnboardingAction,
                onListClick = onListClick,
                onListMenuAction = onListMenuAction,
                onListSelectionChange = onListSelectionChange,
                onPageClick = onPageClick,
                onPageLongClick = onPageLongClick,
                onPageChipClick = onPageChipClick
            )
        }
    } else {
        ReadingListsContent(
            uiState = uiState,
            isSelectionMode = isSelectionMode,
            selectedListIds = selectedListIds,
            onOnboardingAction = onOnboardingAction,
            onListClick = onListClick,
            onListMenuAction = onListMenuAction,
            onListSelectionChange = onListSelectionChange,
            onPageClick = onPageClick,
            onPageLongClick = onPageLongClick,
            onPageChipClick = onPageChipClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ReadingListsContent(
    uiState: ReadingListsUiState,
    isSelectionMode: Boolean,
    selectedListIds: Set<Long>,
    onOnboardingAction: (OnboardingAction) -> Unit,
    onListClick: (Long) -> Unit,
    onListMenuAction: (Long, ReadingListMenuAction) -> Unit,
    onListSelectionChange: (Long) -> Unit,
    onPageClick: (Long) -> Unit,
    onPageLongClick: (Long) -> Unit,
    onPageChipClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.rows.isEmpty() && uiState.searchQuery.isNullOrEmpty() && uiState.onboarding == OnboardingState.None -> {
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
                onboarding = uiState.onboarding,
                onOnboardingAction = onOnboardingAction,
                isSelectionMode = isSelectionMode,
                selectedListIds = selectedListIds,
                onListClick = onListClick,
                onListMenuAction = onListMenuAction,
                onListSelectionChange = onListSelectionChange,
                onPageClick = onPageClick,
                onPageLongClick = onPageLongClick,
                onPageChipClick = onPageChipClick,
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
    onboarding: OnboardingState,
    onOnboardingAction: (OnboardingAction) -> Unit,
    isSelectionMode: Boolean,
    selectedListIds: Set<Long>,
    onListClick: (Long) -> Unit,
    onListMenuAction: (Long, ReadingListMenuAction) -> Unit,
    onListSelectionChange: (Long) -> Unit,
    onPageClick: (Long) -> Unit,
    onPageLongClick: (Long) -> Unit,
    onPageChipClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        if (onboarding != OnboardingState.None) {
            item(key = "onboarding") {
                OnboardingCard(
                    state = onboarding,
                    onAction = onOnboardingAction
                )
            }
        }
        items(
            items = rows,
            key = { row ->
                when (row) {
                    is ReadingListRow.ListRow -> "list-${row.list.id}"
                    is ReadingListRow.PageRow -> "page-${row.page.id}"
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
                    containingLists = row.containingLists,
                    onClick = { onPageClick(row.page.id) },
                    onLongClick = { onPageLongClick(row.page.id) },
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
private fun ReadingListsComposeScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ReadingListsComposeScreen(
            uiState = ReadingListsUiState(
                rows = listOf(
                    ReadingListRow.ListRow(ReadingListUiModel(id = 1, title = "Default", description = null, isDefault = true, totalPages = 3, sizeBytesFromPages = 0)),
                    ReadingListRow.ListRow(ReadingListUiModel(id = 2, title = "Physics", description = "reading", isDefault = false, totalPages = 12, sizeBytesFromPages = 1240000))
                )
            )
        )
    }
}

@Preview
@Composable
private fun ReadingListsEmptyPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadingListsComposeScreen(
            uiState = ReadingListsUiState()
        )
    }
}

@Preview
@Composable
private fun ReadingListsOnboardingPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ReadingListsComposeScreen(
            uiState = ReadingListsUiState(
                rows = listOf(
                    ReadingListRow.ListRow(ReadingListUiModel(id = 2, title = "Physics", description = "reading", isDefault = false, totalPages = 12, sizeBytesFromPages = 1240000))
                ),
                onboarding = OnboardingState.RecommendedReadingList
            )
        )
    }
}
