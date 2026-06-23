package org.wikipedia.feed.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.FeedFeatureTeaserModule
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.PlacesOfInterestCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme

private val placesPromptImageUrls = listOf(
    "https://upload.wikimedia.org/wikipedia/commons/thumb/1/16/Pyramids_of_Giza%2C_Giza%2C_GG%2C_EGY_%2846986591195%29.jpg/960px-Pyramids_of_Giza%2C_Giza%2C_GG%2C_EGY_%2846986591195%29.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2c/Pavillon_Sully_du_Louvre_002.jpg/960px-Pavillon_Sully_du_Louvre_002.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Rio_de_Janeiro%2C_Brazil_003_version_2.jpg/960px-Rio_de_Janeiro%2C_Brazil_003_version_2.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4f/Snowy_Mountains_In_Sky_%28Unsplash%29.jpg/960px-Snowy_Mountains_In_Sky_%28Unsplash%29.jpg"
)

@Composable
fun PlacesOfInterestLocationPromptModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    onGoToPlacesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    FeedFeatureTeaserModule(
        modifier = modifier,
        title = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_cta_title),
        description = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_cta_description),
        buttonText = context.getString(wikiSite.languageCode, R.string.home_feed_places_of_interest_cta_button),
        buttonIcon = painterResource(R.drawable.ic_location_on_filled_24dp),
        imageUrls = placesPromptImageUrls,
        onButtonClick = onGoToPlacesClick
    )
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
    onCustomizeClick: (card: Card) -> Unit = {},
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
            onCustomizeClick = { onCustomizeClick(card) }
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
