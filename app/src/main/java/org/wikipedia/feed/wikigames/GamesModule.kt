package org.wikipedia.feed.wikigames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import org.wikipedia.feed.FeedFeatureTeaserModule
import org.wikipedia.feed.ForYouCardDropdownMenu
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.GamesModulePromptCard
import org.wikipedia.feed.model.WikiGameCard
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

private val promptImageUrls = listOf(
    "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Red_eyed_tree_frog_edit2.jpg/500px-Red_eyed_tree_frog_edit2.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/9/94/Palais_de_l%27Industrie_-_%C3%89douard_Baldus.jpg/500px-Palais_de_l%27Industrie_-_%C3%89douard_Baldus.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/1/17/Monet_w861.jpg/500px-Monet_w861.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/3/30/Mercury_in_color_-_Prockter07_centered.jpg/500px-Mercury_in_color_-_Prockter07_centered.jpg"
)

@Composable
fun GamesModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.Games,
    onGameActionClick: (game: WikiGame) -> Unit,
    onGoToGamesHubClick: () -> Unit,
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit,
    onHideModuleClick: () -> Unit,
    onCardInView: (card: Card) -> Unit,
    onCustomizeInterestsClick: (card: Card) -> Unit
) {
    val context = LocalContext.current

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { page ->
        val spacingForPagerDots = if (module.cards.size > 1) 40.dp else 16.dp
        val cardModifier = Modifier.padding(horizontal = 16.dp)
        when (val card = module.cards[page]) {
            is WikiGameCard -> when (val game = card.wikiGame) {
                is WikiGame.OnThisDayGame -> OnThisDayGameModuleCard(
                    modifier = cardModifier,
                    wikiSite = wikiSite,
                    state = game.state,
                    bottomSpacing = spacingForPagerDots,
                    onActionClick = { onGameActionClick(game) },
                    onHideCardClick = { onHideCardClick(module, card) },
                    onHideModuleClick = onHideModuleClick,
                    onCustomizeInterestsClick = { onCustomizeInterestsClick(card) }
                )
            }
            else -> {
                FeedFeatureTeaserModule(
                    modifier = cardModifier,
                    title = context.getString(wikiSite.languageCode, R.string.home_feed_games_module_cta_prompt_title),
                    description = context.getString(wikiSite.languageCode, R.string.home_feed_games_module_cta_prompt_subtitle),
                    buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_games_module_cta_prompt_button_text),
                    buttonIcon = painterResource(R.drawable.ic_esports_24),
                    imageUrls = promptImageUrls,
                    bottomSpacing = spacingForPagerDots,
                    onButtonClick = onGoToGamesHubClick
                )
            }
        }
    }
}

@Composable
private fun OnThisDayGameModuleCard(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    state: OnThisDayCardGameState,
    bottomSpacing: Dp,
    onActionClick: () -> Unit,
    onHideCardClick: () -> Unit,
    onHideModuleClick: () -> Unit,
    onCustomizeInterestsClick: () -> Unit
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
            .padding(top = 16.dp, bottom = bottomSpacing),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = context.getString(wikiSite.languageCode, R.string.on_this_day_game_title),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                maxLines = 1
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
                    onShareClick = null,
                    onSaveClick = null,
                    onHideCardClick = onHideCardClick,
                    onHideModuleClick = onHideModuleClick,
                    onCustomizeClick = onCustomizeInterestsClick
                )
            }
        }

        GameEventCard(modifier = Modifier.weight(1f), event = state.event1, onClick = onActionClick)

        GameEventCard(modifier = Modifier.weight(1f), event = state.event2, onClick = onActionClick)

        OutlinedButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onActionClick,
            shape = RoundedCornerShape(percent = 50),
            border = BorderStroke(1.dp, Color.White),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text = context.getString(wikiSite.languageCode, buttonTextRes),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun GameEventCard(
    modifier: Modifier = Modifier,
    event: OnThisDay.Event,
    onClick: () -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ComposeColors.Gray700)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                    WikiGameCard(
                        WikiGame.OnThisDayGame(
                            OnThisDayCardGameState.Preview(
                                langCode = "en",
                                event1 = OnThisDay.Event(text = "U.S. figure skater Nancy Kerrigan is attacked and injured by an assailant.", year = 1994),
                                event2 = OnThisDay.Event(text = "Americans storm the United States Capitol Building to disrupt certification of the 2020 presidential election.", year = 2021)
                            )
                        ),
                        date = "2024-01-06"
                    )
                )
            ),
            onGameActionClick = {},
            onHideModuleClick = {},
            onCardInView = { },
            onGoToGamesHubClick = {},
            onHideCardClick = { _, _ -> },
            onCustomizeInterestsClick = { _ -> }
        )
    }
}

@Preview
@Composable
private fun GamesModulePromptPreview() {
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
                    GamesModulePromptCard()
                )
            ),
            onGameActionClick = {},
            onHideModuleClick = {},
            onCardInView = { },
            onGoToGamesHubClick = {},
            onHideCardClick = { _, _ -> },
            onCustomizeInterestsClick = { _ -> }
        )
    }
}
