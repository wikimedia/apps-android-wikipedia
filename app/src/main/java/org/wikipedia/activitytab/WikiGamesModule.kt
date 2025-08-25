package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState

@Composable
fun WikiGamesModule(
    modifier: Modifier = Modifier,
    uiState: UiState<OnThisDayGameViewModel.GameStatistics?>,
    onEntryCardClick: (() -> Unit)? = null,
    onStatsCardClick: (() -> Unit)? = null,
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    if (uiState == UiState.Loading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    } else if (uiState is UiState.Success) {
        if (uiState.data == null) {
            WikiGamesEntryCard(
                modifier = modifier
                    .fillMaxWidth(),
                onClick = onEntryCardClick
            )
        } else {
            WikiGamesStatsCard(
                modifier = modifier
                    .fillMaxWidth(),
                gameStatistics = uiState.data,
                onClick = onStatsCardClick
            )
        }
    } else if (uiState is UiState.Error) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WikiErrorView(
                modifier = Modifier
                    .fillMaxWidth(),
                caught = uiState.error,
                errorClickEvents = wikiErrorClickEvents
            )
        }
    }
}

@Composable
fun WikiGamesStatsCard(
    modifier: Modifier = Modifier,
    gameStatistics: OnThisDayGameViewModel.GameStatistics,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.activity_tab_game_stats),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = WikipediaTheme.colors.primaryColor,
                    lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                )
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                    tint = WikipediaTheme.colors.secondaryColor,
                    contentDescription = null
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.baseline_extension_24,
                    statValue = gameStatistics.totalGamesPlayed.toString(),
                    statLabel = pluralStringResource(R.plurals.on_this_day_game_stats_games_played, gameStatistics.totalGamesPlayed)
                )
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.outline_motion_blur_24,
                    statValue = gameStatistics.currentStreak.toString(),
                    statLabel = stringResource(R.string.on_this_day_game_stats_streak)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.filled_family_star_24,
                    statValue = gameStatistics.bestStreak.toString(),
                    statLabel = stringResource(R.string.activity_tab_game_stats_best_streak)
                )
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.outline_sports_score_24,
                    statValue = gameStatistics.averageScore.toString(),
                    statLabel = stringResource(R.string.on_this_day_game_stats_average_score)
                )
            }
        }
    }
}

@Composable
fun WikiGamesStatView(
    modifier: Modifier,
    iconResource: Int,
    statValue: String,
    statLabel: String
) {
    Row(
        modifier = modifier
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(iconResource),
            tint = WikipediaTheme.colors.progressiveColor,
            contentDescription = null
        )
        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = statValue,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = statLabel.lowercase(),
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    }
}

@Composable
fun WikiGamesEntryCard(
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        elevation = 4.dp,
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.progressiveColor,
            contentColor = WikipediaTheme.colors.progressiveColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .heightIn(min = 116.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.on_this_day_game_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        lineHeight = 24.sp
                    ),
                    color = WikipediaTheme.colors.paperColor,
                    fontFamily = FontFamily.Serif,
                )
                HtmlText(
                    text = stringResource(R.string.on_this_day_game_splash_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.paperColor
                )
            }
            Icon(
                modifier = Modifier.size(44.dp),
                painter = painterResource(R.drawable.ic_today_24px),
                tint = WikipediaTheme.colors.paperColor,
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
private fun WikiGamesModulePreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesModule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            uiState = UiState.Error(Throwable("Error")),
            onEntryCardClick = {},
            onStatsCardClick = {},
            wikiErrorClickEvents = null
        )
    }
}

@Preview
@Composable
private fun WikiGamesEntryCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesEntryCard(
            modifier = Modifier,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun WikiGamesStatViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesStatView(
            modifier = Modifier,
            iconResource = R.drawable.ic_today_24px,
            statValue = "42",
            statLabel = pluralStringResource(R.plurals.on_this_day_game_stats_games_played, 42)
        )
    }
}

@Preview
@Composable
private fun WikiGamesStatsCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesStatsCard(
            modifier = Modifier,
            gameStatistics = OnThisDayGameViewModel.GameStatistics(
                totalGamesPlayed = 43,
                averageScore = 4.5,
                currentStreak = 5,
                bestStreak = 15
            ),
            onClick = {}
        )
    }
}
