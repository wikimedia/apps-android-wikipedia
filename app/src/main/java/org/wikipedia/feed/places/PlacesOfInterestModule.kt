package org.wikipedia.feed.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.PlacesOfInterestCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme

private const val SHORT_TILE_WEIGHT = 100f
private const val TALL_TILE_WEIGHT = 171f
private const val IMAGE_GIZA = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Pyramids_of_Giza%2C_Giza%2C_GG%2C_EGY_%2846986591195%29.jpg/960px-Pyramids_of_Giza%2C_Giza%2C_GG%2C_EGY_%2846986591195%29.jpg"
private const val IMAGE_RIO = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Rio_de_Janeiro%2C_Brazil_003_version_2.jpg/960px-Rio_de_Janeiro%2C_Brazil_003_version_2.jpg"
private const val IMAGE_LOUVRE = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Pavillon_Sully_du_Louvre_002.jpg/960px-Pavillon_Sully_du_Louvre_002.jpg"
private const val IMAGE_SNOWY_MOUNTAIN = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Snowy_Mountains_In_Sky_%28Unsplash%29.jpg/960px-Snowy_Mountains_In_Sky_%28Unsplash%29.jpg"

@Composable
fun PlacesOfInterestLocationPromptModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    onGoToPlacesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        PlaceholderImageGrid(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_cta_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_cta_description),
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.25.sp),
            color = WikipediaTheme.colors.primaryColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onGoToPlacesClick,
            border = BorderStroke(1.dp, WikipediaTheme.colors.primaryColor)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_location_on_filled_24dp),
                contentDescription = null,
                tint = WikipediaTheme.colors.primaryColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_cta_button),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PlacesOfInterestArticlesModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.PlacesOfInterest,
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeInterestsClick: (card: Card) -> Unit = {},
) {
    val context = LocalContext.current
    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { pageIndex ->
        val card = module.cards[pageIndex] as PlacesOfInterestCard
        val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_FEED_PLACES)
        ForYouCardContent(
            wikiSite = wikiSite,
            title = card.title,
            module = module,
            card = card,
            footerIcon = painterResource(R.drawable.ic_location_on_24dp),
            footerText = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_card_footer, card.distance),
            onPageClick = { onPageClick(card, historyEntry) },
            onShareClick = { onPageShareClick(card, historyEntry) },
            onSaveClick = { onPageBookmarkClick(card, historyEntry) },
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeInterestsClick = { onCustomizeInterestsClick(card) }
        )
    }
}
@Composable
private fun PlaceholderImageGrid(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlaceholderTile(modifier = Modifier.weight(SHORT_TILE_WEIGHT), imageUrl = IMAGE_GIZA)
            PlaceholderTile(modifier = Modifier.weight(TALL_TILE_WEIGHT), imageUrl = IMAGE_LOUVRE)
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlaceholderTile(modifier = Modifier.weight(TALL_TILE_WEIGHT), imageUrl = IMAGE_RIO)
            PlaceholderTile(modifier = Modifier.weight(SHORT_TILE_WEIGHT), imageUrl = IMAGE_SNOWY_MOUNTAIN)
        }
    }
}

@Composable
private fun PlaceholderTile(
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
            error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview
@Composable
private fun PlacesOfInterestLocationPromptModulePreview() {
    BaseTheme(currentTheme = Theme.DARK) {
        PlacesOfInterestLocationPromptModule(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColors.Green800)
                .padding(horizontal = 16.dp),
            wikiSite = WikiSite.preview()
        )
    }
}

@Preview
@Composable
private fun PlacesOfInterestArticlesModulePreview() {
    val title = PageTitle(
        text = "Eiffel Tower",
        displayText = "Eiffel Tower",
        wiki = WikiSite.preview(),
        description = "Wrought-iron lattice tower in Paris",
        extract = "The Eiffel Tower is a wrought-iron lattice tower on the Champ de Mars in Paris, France.",
        thumbUrl = "https://example.com/thumb.jpg"
    )
    val card = PlacesOfInterestCard(title, distance = "10 km")
    BaseTheme(currentTheme = Theme.LIGHT) {
        PlacesOfInterestArticlesModule(
            modifier = Modifier.fillMaxSize(),
            wikiSite = WikiSite.preview(),
            module = ForYouModule.PlacesOfInterest(0, 0, listOf(card, card, card, card), hasLocationPermission = true)
        )
    }
}
