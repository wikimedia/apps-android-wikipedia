package org.wikipedia.activitytab

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import java.text.DecimalFormat

@Composable
fun WikiGamesModule(
    modifier: Modifier = Modifier,
    uiState: UiState<OnThisDayGameViewModel.GameStatistics?>,
    onPlayGameCardClick: (() -> Unit)? = null,
    onStatsCardClick: (() -> Unit)? = null,
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    when (uiState) {
        UiState.Loading -> {
            ActivityTabShimmerView(size = 160.dp)
        }
        is UiState.Success -> {
            WikiGamesStatsCard(
                modifier = modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                totalGamesPlayed = uiState.data?.totalGamesPlayed ?: 0,
                currentStreak = uiState.data?.currentStreak ?: 0,
                bestStreak = uiState.data?.bestStreak ?: 0,
                averageScore = uiState.data?.averageScore ?: 0.0,
                onStatsCardClick = onStatsCardClick,
                onPlayGameCardClick = onPlayGameCardClick
            )
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WikiErrorView(
                    modifier = Modifier
                        .fillMaxWidth(),
                    caught = uiState.error,
                    errorClickEvents = wikiErrorClickEvents,
                    retryForGenericError = true
                )
            }
        }
    }
}

@Composable
fun WikiGamesStatsCard(
    modifier: Modifier = Modifier,
    totalGamesPlayed: Int = 0,
    currentStreak: Int = 0,
    bestStreak: Int = 0,
    averageScore: Double = 0.0,
    showTitle: Boolean = true,
    onStatsCardClick: (() -> Unit)? = null,
    onPlayGameCardClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        onClick = onStatsCardClick
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showTitle) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
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
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.baseline_extension_24,
                    statValue = totalGamesPlayed.toString(),
                    statLabel = pluralStringResource(R.plurals.on_this_day_game_stats_games_played, totalGamesPlayed)
                )
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.outline_motion_blur_24,
                    statValue = if (currentStreak == 0) "-" else currentStreak.toString(),
                    statLabel = stringResource(R.string.on_this_day_game_stats_streak)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.filled_family_star_24,
                    statValue = if (bestStreak == 0) "-" else bestStreak.toString(),
                    statLabel = stringResource(R.string.activity_tab_game_stats_best_streak)
                )
                WikiGamesStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.outline_sports_score_24,
                    statValue = if (averageScore == 0.0) "-" else DecimalFormat("0.#").format(averageScore),
                    statLabel = stringResource(R.string.on_this_day_game_stats_average_score)
                )
            }
            if (totalGamesPlayed == 0) {
                Button(
                    modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor,
                        contentColor = Color.White,
                    ),
                    onClick = { onPlayGameCardClick?.invoke() },
                ) {
                    Text(
                        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
                        text = stringResource(R.string.activity_tab_play_wiki_games),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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

@Preview(showBackground = true)
@Composable
private fun WikiGamesModulePreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesModule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            uiState = UiState.Success(
                OnThisDayGameViewModel.GameStatistics(
                    totalGamesPlayed = 43,
                    averageScore = 4.5,
                    currentStreak = 5,
                    bestStreak = 15
                )
            ),
            onPlayGameCardClick = {},
            onStatsCardClick = {},
            wikiErrorClickEvents = null
        )
    }
}

@Preview
@Composable
private fun WikiGamesModuleWithPlayButtonPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesModule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            uiState = UiState.Success(
                OnThisDayGameViewModel.GameStatistics(
                    totalGamesPlayed = 0,
                    averageScore = 0.0,
                    currentStreak = 0,
                    bestStreak = 0
                )
            ),
            onPlayGameCardClick = {},
            onStatsCardClick = {},
            wikiErrorClickEvents = null
        )
    }
}
