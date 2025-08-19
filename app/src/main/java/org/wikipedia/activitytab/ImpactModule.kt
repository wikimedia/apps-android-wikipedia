package org.wikipedia.activitytab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact.ArticleViews
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState

@Composable
fun ImpactModule(
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
    }
}

@Composable
fun MostViewedCard(
    modifier: Modifier = Modifier,
    topViewedArticles: Map<String, ArticleViews>,
    onClick: (() -> Unit)? = null
) {
    // TODO: Implement the UI for the Most Viewed Card
}

@Composable
fun ContributionCard(
    modifier: Modifier = Modifier,
    groupEditsByMonth: Map<String, Int>,
    onClick: (() -> Unit)? = null
) {
    // TODO: Implement the UI for the ContributionCard
}

@Composable
fun EditsStatsCard(
    modifier: Modifier = Modifier,
    impact: GrowthUserImpact,
    onClick: (() -> Unit)? = null
) {
    // TODO: Implement the UI for the EditsStatsCard
}

@Composable
fun SuggestedEditsStatView(
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
