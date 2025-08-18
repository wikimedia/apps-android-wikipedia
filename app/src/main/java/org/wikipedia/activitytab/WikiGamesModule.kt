package org.wikipedia.activitytab

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
    onClick: (() -> Unit)? = null,
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        elevation = 0.dp
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
                    onClick = onClick
                )
            } else {
                WikiGamesEntryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onClick
                )
            }
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
                    text = stringResource(R.string.on_this_day_game_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = WikipediaTheme.colors.paperColor,
                    fontFamily = FontFamily.Serif
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
private fun DonationModulePreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WikiGamesModule(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            uiState = UiState.Success(null),
            onClick = {},
            wikiErrorClickEvents = null
        )
    }
}
