package org.wikipedia.games.onthisday

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun OnThisDayGameLoginPromptCard(
    modifier: Modifier = Modifier,
    onLogInClick: () -> Unit,
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
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.on_this_day_game_stats_log_in_prompt_card_view_stats_label),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                modifier = Modifier
                    .padding(top = 8.dp),
                text = stringResource(R.string.on_this_day_game_login_in_prompt_card_view_stats_label),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                modifier = Modifier
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WikipediaTheme.colors.progressiveColor,
                    contentColor = Color.White,
                ),
                onClick = onLogInClick,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(18.dp),
                        painter = painterResource(R.drawable.ic_person_filled_24),
                        contentDescription = null
                    )
                    Text(
                        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
                        text = stringResource(R.string.on_this_day_game_stats_log_in_prompt_card_btn_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnThisDayGameLoginPromptCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnThisDayGameLoginPromptCard(
            onLogInClick = {}
        )
    }
}
