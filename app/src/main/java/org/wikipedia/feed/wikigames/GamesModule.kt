package org.wikipedia.feed.wikigames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.ForYouCardDropdownMenu
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.LoadingIndicator
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.OnThisDayGameCard
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun GamesModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.Games,
    onActionClick: (state: OnThisDayCardGameState) -> Unit = {},
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {}
) {
    if (module.isLoading) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
        return
    }

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { page ->
        // TODO: handle the "Games hub" promo card here (page 2) when it is added.
        when (val card = module.cards[page]) {
            is OnThisDayGameCard -> OnThisDayGameModuleCard(
                modifier = Modifier.fillMaxSize(),
                wikiSite = wikiSite,
                state = card.state,
                onActionClick = { onActionClick(card.state) },
                onHideModuleClick = onHideModuleClick
            )
            else -> {}
        }
    }
}

@Composable
private fun OnThisDayGameModuleCard(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    state: OnThisDayCardGameState,
    onActionClick: () -> Unit,
    onHideModuleClick: () -> Unit
) {
    val context = LocalContext.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    val buttonTextRes = when (state) {
        is OnThisDayCardGameState.Preview -> R.string.on_this_day_game_play_today_btn_text
        is OnThisDayCardGameState.InProgress -> R.string.on_this_day_game_continue_btn_text
        is OnThisDayCardGameState.Completed -> R.string.on_this_day_game_review_results_btn_text
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = context.getString(wikiSite.languageCode, R.string.on_this_day_game_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(onClick = { overflowMenuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        contentDescription = context.getString(wikiSite.languageCode, R.string.menu_feed_overflow_label),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                ForYouCardDropdownMenu(
                    expanded = overflowMenuExpanded,
                    wikiSite = wikiSite,
                    onDismiss = { overflowMenuExpanded = false },
                    onShareClick = {},
                    onSaveClick = {},
                    onHideCardClick = {},
                    onHideModuleClick = onHideModuleClick,
                    onCustomizeInterestsClick = {}
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        GameEventCard(modifier = Modifier.weight(1f), event = state.event1)
        Spacer(Modifier.height(12.dp))
        GameEventCard(modifier = Modifier.weight(1f), event = state.event2)

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onActionClick,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text = context.getString(wikiSite.languageCode, buttonTextRes),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun GameEventCard(
    modifier: Modifier = Modifier,
    event: OnThisDay.Event
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ComposeColors.Gray700)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = event.text,
                color = ComposeColors.Gray200,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, letterSpacing = 0.sp),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
            OnThisDayGameProvider.getThumbnailUrlForEvent(event)?.let { thumbnailUrl ->
                AsyncImage(
                    model = ImageService.getRequest(LocalContext.current, url = thumbnailUrl),
                    error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Preview
@Composable
private fun GamesModulePreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        GamesModule(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColors.Green800),
            wikiSite = WikiSite.preview(),
            module = ForYouModule.Games(
                age = 0,
                index = 0,
                cards = listOf(
                    OnThisDayGameCard(
                        OnThisDayCardGameState.Preview(
                            langCode = "en",
                            event1 = OnThisDay.Event(text = "U.S. figure skater Nancy Kerrigan is attacked and injured by an assailant.", year = 1994),
                            event2 = OnThisDay.Event(text = "Americans storm the United States Capitol Building to disrupt certification of the 2020 presidential election.", year = 2021)
                        )
                    )
                )
            )
        )
    }
}
