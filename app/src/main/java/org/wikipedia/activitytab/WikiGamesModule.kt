package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
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
                // TODO: show the WikiGames entry card
            } else {
                // TODO: show the WikiGames stats card
            }
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
