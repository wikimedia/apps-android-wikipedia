package org.wikipedia.feed.wikigames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.views.imageservice.ImageService

@Composable
fun WhichCameFirstScreen(
    game: WikiGame.WhichCameFirst,
    onPlayClick: () -> Unit,
    onCardClick: () -> Unit,
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
            WhichCameFirstEventView(event = game.event1)
            WhichCameFirstEventView(event = game.event2)
        }
    }
}

@Composable
fun WhichCameFirstEventView(
    event: OnThisDay.Event
) {
    Row(
        modifier = Modifier
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f),
            text = event.text,
            color = WikipediaTheme.colors.secondaryColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            )
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
