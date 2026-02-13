package org.wikipedia.feed.wikigames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun OnThisDayGameCardPreview(
    game: OnThisDayCardGameState.Preview,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WikiCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    modifier = Modifier
                        .size(24.dp),
                    painter = painterResource(R.drawable.ic_wiki_games_events),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.on_this_day_game_title),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.15.sp
                    )
                )
            }

            OnThisDayGameFirstEventView(event = game.event1)
            OnThisDayGameFirstEventView(event = game.event2)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                FilledTonalButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WikipediaTheme.colors.backgroundColor,
                        contentColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onPlayClick
                ) {
                    Text(
                        text = stringResource(R.string.on_this_day_game_entry_dialog_button),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun OnTHisDayCardProgress(
    modifier: Modifier = Modifier,
    game: OnThisDayCardGameState.InProgress,
    onContinueClick: () -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Icon(
                modifier = Modifier
                    .size(44.dp),
                painter = painterResource(R.drawable.ic_wiki_games_events),
                tint = WikipediaTheme.colors.primaryColor,
                contentDescription = null
            )

            Text(
                modifier = Modifier
                    .padding(top = 16.dp),
                text = stringResource(R.string.on_this_day_game_title),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp
                )
            )

            Text(
                modifier = Modifier,
                text = stringResource(R.string.on_this_day_game_card_progress_label, game.currentQuestion + 1),
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp,
                    letterSpacing = 0.sp
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp)
            ) {
                FilledTonalButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WikipediaTheme.colors.backgroundColor,
                        contentColor = WikipediaTheme.colors.progressiveColor
                    ),
                    onClick = onContinueClick
                ) {
                    Text(
                        text = stringResource(R.string.on_this_day_game_continue_btn_text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun OnThisDayGameFirstEventView(
    event: OnThisDay.Event
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = event.text,
            color = WikipediaTheme.colors.secondaryColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        AsyncImage(
            model = ImageService.getRequest(LocalContext.current, url = OnThisDayGameProvider.getThumbnailUrlForEvent(event)),
            error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

@Preview
@Composable
private fun OnTHisDayCardProgressPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        OnTHisDayCardProgress(
            game = OnThisDayCardGameState.InProgress(3),
            onContinueClick = {}
        )
    }
}
