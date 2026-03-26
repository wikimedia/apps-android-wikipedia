package org.wikipedia.games

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.WikiGamesEvent
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.extensions.shimmerEffect
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.extensions.getString
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.feed.wikigames.OnThisDayGameAction
import org.wikipedia.feed.wikigames.OnThisDayGameCardCompleted
import org.wikipedia.feed.wikigames.OnThisDayGameCardPreview
import org.wikipedia.feed.wikigames.OnThisDayGameCardProgress
import org.wikipedia.feed.wikigames.OnThisDayGameCardSimple
import org.wikipedia.feed.wikigames.WikiGame
import org.wikipedia.games.onthisday.OnThisDayGameArchiveCalendarHelper
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UiState
import java.time.LocalDate

@Composable
fun GamesHubScreen(
    viewModel: GamesHubViewModel,
    onThisDayGameUiState: UiState<List<OnThisDayCardGameState>>,
    onThisDayGameArchiveCalendarHelper: OnThisDayGameArchiveCalendarHelper,
    onPlay: (LocalDate, OnThisDayGameAction) -> Unit,
    onShowArchive: () -> Unit,
    onShowDisabledMessage: (String) -> Unit,
    languageList: List<String>,
    transition: InfiniteTransition
) {
    var selectedLanguage by remember { mutableStateOf(viewModel.selectedLanguage) }
    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor),
        containerColor = WikipediaTheme.colors.paperColor
    ) { paddingValues ->

        PullToRefreshBox(
            onRefresh = {
                WikiGamesEvent.submit(action = "refresh", activeInterface = "games_hub", langCode = selectedLanguage)
                isRefreshing = true
                viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(languageList.size) { index ->
                        val langCode = languageList[index]
                        GamesHubLanguageChip(
                            isSelected = langCode == selectedLanguage,
                            langCode = langCode,
                            onShowDisabledMessage = { snackbarMessage ->
                                WikiGamesEvent.submit(action = "language_chip_select_disabled", activeInterface = "games_hub", langCode = langCode)
                                onShowDisabledMessage(snackbarMessage)
                            },
                            onSelected = {
                                WikiGamesEvent.submit(action = "language_chip_select", activeInterface = "games_hub", langCode = langCode)
                                selectedLanguage = langCode
                                viewModel.selectedLanguage = langCode
                                viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                                onThisDayGameArchiveCalendarHelper.updateLanguageCode(selectedLanguage)
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    items(WikiGames.entries.size) { index ->
                        when (WikiGames.entries[index]) {
                            WikiGames.WHICH_CAME_FIRST -> {
                                when (onThisDayGameUiState) {
                                    is UiState.Loading -> {
                                        GamesHubLoadingShimmer(transition = transition)
                                    }

                                    is UiState.Error -> {
                                        isRefreshing = false
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp, vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            WikiErrorView(
                                                modifier = Modifier
                                                    .fillMaxWidth(),
                                                caught = onThisDayGameUiState.error,
                                                errorClickEvents = WikiErrorClickEvents {
                                                    isRefreshing = true
                                                    viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                                                },
                                                retryForGenericError = true
                                            )
                                        }
                                    }

                                    is UiState.Success -> {
                                        isRefreshing = false
                                        OnThisDayGameCards(
                                            position = index,
                                            selectedLanguage = selectedLanguage,
                                            gamesData = onThisDayGameUiState.data,
                                            onThisDayGameAction = { gameStatus, gameDate ->
                                                when (gameStatus) {
                                                    OnThisDayGameAction.PlayArchive -> {
                                                        onShowArchive()
                                                    }
                                                    OnThisDayGameAction.CountdownFinished -> {
                                                        isRefreshing = true
                                                        viewModel.loadOnThisDayGamesPreviews(selectedLanguage)
                                                    }
                                                    else -> {
                                                        onPlay(gameDate, gameStatus)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GamesHubLoadingShimmer(
    transition: InfiniteTransition
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
                .height(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(
                    heightMultiplier = 0f,
                    transition = transition
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp
                )
                .height(350.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect(
                    heightMultiplier = 0f,
                    transition = transition
                )
        )
    }
}

@Composable
fun GamesHubLanguageChip(
    isSelected: Boolean,
    langCode: String,
    onShowDisabledMessage: (String) -> Unit,
    onSelected: () -> Unit,
) {
    val langText = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(langCode) ?: langCode
    val isEnabled = WikiGames.WHICH_CAME_FIRST.isLangSupported(langCode) // TODO: Add check for other games when they are added
    val textColor = if (isEnabled) WikipediaTheme.colors.primaryColor else WikipediaTheme.colors.inactiveColor
    val snackbarMessage = stringResource(R.string.games_hub_activity_games_unavailable_message)
    FilterChip(
        selected = isSelected,
        onClick = {
            if (!isEnabled) {
                onShowDisabledMessage(snackbarMessage)
                return@FilterChip
            }
            onSelected()
        },
        colors = FilterChipDefaults.filterChipColors().copy(
            selectedContainerColor = WikipediaTheme.colors.additionColor
        ),
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        label = {
            Text(
                text = langText,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_black_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
fun OnThisDayGameCards(
    position: Int,
    selectedLanguage: String,
    gamesData: List<OnThisDayCardGameState>,
    onThisDayGameAction: (OnThisDayGameAction, LocalDate) -> Unit
) {
    if (WikiGames.WHICH_CAME_FIRST.isLangSupported(selectedLanguage)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            text = LocalContext.current.getString(selectedLanguage, WikiGames.entries[position].titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )

        // Load fix cards: Today, last 3 days and archive
        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(5) { cardIndex ->
                val gameDate = LocalDate.now().minusDays(cardIndex.toLong())
                val dateTitle = DateUtil.getMonthOnlyDateString(gameDate)
                when (cardIndex) {
                    in 0..3 -> {
                        val gameState = gamesData.getOrNull(cardIndex) ?: return@items
                        val isArchiveGame = cardIndex != 0
                        OnThisDayGameCardContent(
                            modifier = Modifier
                                .width(if (!isArchiveGame) 320.dp else 170.dp)
                                .height(350.dp)
                                .padding(vertical = 8.dp),
                            game = WikiGame.OnThisDayGame(gameState),
                            dateTitle = dateTitle,
                            isArchiveGame = isArchiveGame,
                            selectedLanguage = selectedLanguage,
                            cardPosition = cardIndex,
                            onThisDayGameAction = {
                                onThisDayGameAction(it, gameDate)
                            }
                        )
                    }
                    else -> {
                        OnThisDayGameCardSimple(
                            modifier = Modifier
                                .width(170.dp)
                                .height(350.dp)
                                .padding(vertical = 8.dp),
                            iconRes = R.drawable.event_repeat_24dp,
                            iconTint = WikipediaTheme.colors.primaryColor,
                            titleText = stringResource(R.string.on_this_day_game_card_archive_label),
                            onPlayClick = {
                                WikiGamesEvent.submit(action = "play_click", activeInterface = "games_hub", cardType = "archive", langCode = selectedLanguage, position = cardIndex + 1)
                                onThisDayGameAction(OnThisDayGameAction.PlayArchive, gameDate)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnThisDayGameCardContent(
    modifier: Modifier,
    game: WikiGame.OnThisDayGame,
    dateTitle: String,
    isArchiveGame: Boolean,
    onThisDayGameAction: (OnThisDayGameAction) -> Unit,
    selectedLanguage: String,
    cardPosition: Int
) {
    val cardPositionForEvent = if (isArchiveGame) cardPosition + 1 else null
    val cardTypeForEvent = if (isArchiveGame) "archive" else "today"

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        when (game.state) {
            is OnThisDayCardGameState.Preview -> {
                if (!isArchiveGame) {
                    OnThisDayGameCardPreview(
                        modifier = modifier,
                        state = game.state,
                        titleText = dateTitle,
                        onPlayClick = {
                            WikiGamesEvent.submit(action = "play_click", activeInterface = "games_hub", cardType = "today", langCode = selectedLanguage)
                            onThisDayGameAction(OnThisDayGameAction.Play)
                        }
                    )
                } else {
                    OnThisDayGameCardSimple(
                        modifier = modifier,
                        iconRes = R.drawable.ic_events_24dp,
                        iconTint = WikipediaTheme.colors.progressiveColor,
                        titleText = dateTitle,
                        onPlayClick = {
                            WikiGamesEvent.submit(action = "play_click", activeInterface = "games_hub", cardType = "archive", langCode = selectedLanguage, position = cardPositionForEvent)
                            onThisDayGameAction(OnThisDayGameAction.Play)
                        }
                    )
                }
            }

            is OnThisDayCardGameState.InProgress -> {
                OnThisDayGameCardProgress(
                    modifier = modifier,
                    isArchiveGame = isArchiveGame,
                    state = game.state,
                    titleText = dateTitle,
                    onContinueClick = {
                        WikiGamesEvent.submit(action = "continue_click", activeInterface = "games_hub", cardType = cardTypeForEvent, langCode = selectedLanguage, position = cardPositionForEvent)
                        onThisDayGameAction(OnThisDayGameAction.Play)
                    }
                )
            }

            is OnThisDayCardGameState.Completed -> {
                OnThisDayGameCardCompleted(
                    modifier = modifier,
                    isArchiveGame = isArchiveGame,
                    state = game.state,
                    titleText = dateTitle,
                    onPlayClick = {
                        WikiGamesEvent.submit(action = "review_click", activeInterface = "games_hub", cardType = cardTypeForEvent, langCode = selectedLanguage, position = cardPositionForEvent)
                        onThisDayGameAction(OnThisDayGameAction.Play) },
                    onReviewResult = {
                        WikiGamesEvent.submit(action = "review_click", activeInterface = "games_hub", cardType = cardTypeForEvent, langCode = selectedLanguage, position = cardPositionForEvent)
                        onThisDayGameAction(OnThisDayGameAction.ReviewResults) },
                    onPlayTheArchive = {
                        WikiGamesEvent.submit(action = "archive_click", activeInterface = "games_hub", cardType = cardTypeForEvent, langCode = selectedLanguage, position = cardPositionForEvent)
                        onThisDayGameAction(OnThisDayGameAction.PlayArchive) },
                    onCountDownFinished = {
                        onThisDayGameAction(OnThisDayGameAction.CountdownFinished) }
                )
            }
        }
    }
}
