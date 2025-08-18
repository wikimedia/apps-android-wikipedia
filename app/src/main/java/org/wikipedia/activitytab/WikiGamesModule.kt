package org.wikipedia.activitytab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
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
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = onEntryCardClick
            )
        } else {
            WikiGamesStatsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                gameStatistics = uiState.data,
                onClick = onStatsCardClick
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
        elevation = 1.dp,
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
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
                    statLabel = stringResource(R.string.activity_tab_game_stats_played)
                )
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.outline_motion_blur_24,
                    statValue = gameStatistics.currentStreak.toString(),
                    statLabel = stringResource(R.string.activity_tab_game_stats_current_streak)
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
                    iconResource = R.drawable.outline_family_star_24,
                    statValue = gameStatistics.bestStreak.toString(),
                    statLabel = stringResource(R.string.activity_tab_game_stats_best_streak)
                )
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.outline_sports_score_24,
                    statValue = gameStatistics.averageScore.toString(),
                    statLabel = stringResource(R.string.activity_tab_game_stats_average_score)
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
                text = statLabel.toLowerCase(Locale.current),
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    }
}

@Composable
fun WikiGamesEntryCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        elevation = 0.dp,
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.progressiveColor,
            contentColor = WikipediaTheme.colors.progressiveColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
                    .heightIn(min = 132.dp)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.activity_tab_game_entry_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = WikipediaTheme.colors.paperColor,
                    fontFamily = FontFamily.Serif
                )
                HtmlText(
                    text = stringResource(R.string.activity_tab_game_entry_message),
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
private fun WikiGamesEntryCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesEntryCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
        Column(
            modifier = Modifier
                .background(WikipediaTheme.colors.paperColor)
                .padding(16.dp)
        ) {
            WikiGamesStatView(
                modifier = Modifier,
                iconResource = R.drawable.ic_today_24px,
                statValue = "43",
                statLabel = stringResource(R.string.activity_tab_game_stats_played)
            )
        }
    }
}

@Preview
@Composable
private fun WikiGamesStatsCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        Column(
            modifier = Modifier
                .background(WikipediaTheme.colors.paperColor)
                .padding(16.dp)
        ) {
            WikiGamesStatsCard(
                modifier = Modifier.fillMaxWidth(),
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
}
