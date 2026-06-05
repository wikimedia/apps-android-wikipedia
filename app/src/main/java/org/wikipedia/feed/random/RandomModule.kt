package org.wikipedia.feed.random

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.extensions.instrument
import org.wikipedia.feed.CardVariation
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.RandomCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.random.RandomActivity
import org.wikipedia.theme.Theme
import kotlin.math.abs

@Composable
fun RandomModule(
    modifier: Modifier = Modifier,
    wikiSite: WikiSite,
    module: ForYouModule.Random,
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageBookmarkClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onPageShareClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeInterestsClick: (card: Card) -> Unit = {}
) {
    val context = LocalContext.current
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { page ->
        val card = (module.cards[page] as RandomCard)
        val historyEntry = HistoryEntry(card.title, HistoryEntry.SOURCE_FEED_RANDOM)
        ForYouCardContent(
            wikiSite = wikiSite,
            title = card.title,
            variation = CardVariation.entries[page % CardVariation.entries.size],
            backgroundColorIndex = backgroundColorIndex + page,
            module = module,
            card = module.cards[page],
            footerText = context.getString(wikiSite.languageCode, R.string.view_random_article_card_title),
            footerContent = {
                Button(
                    onClick = {
                        context.instrument?.submitInteraction("click", elementId = "random_card_shuffle_button")
                        context.startActivity(RandomActivity.newIntent(context, wikiSite, Constants.InvokeSource.FEED))
                    },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor,
                        contentColor = WikipediaTheme.colors.paperColor,
                    ),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_dice_24),
                            tint = WikipediaTheme.colors.paperColor,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = context.getString(wikiSite.languageCode, R.string.home_feed_random_shuffle_button),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = WikipediaTheme.colors.paperColor
                        )
                    }
                }
            },
            onPageClick = { onPageClick(card, historyEntry) },
            onShareClick = { onPageShareClick(card, historyEntry) },
            onSaveClick = { onPageBookmarkClick(card, historyEntry) },
            onHideCardClick = onHideCardClick,
            onHideModuleClick = onHideModuleClick,
            onCustomizeInterestsClick = { onCustomizeInterestsClick(card) }
        )
    }
}

@Preview
@Composable
fun RandomCardPreviewWithImage() {
    val wikiSite = WikiSite.preview()
    val title = PageTitle(
        text = "Test Article",
        displayText = "Test Article",
        wiki = WikiSite.preview(),
        description = "This is a test article",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbUrl = "https://example.com/thumb.jpg"
    )
    val card = RandomCard(title)
    BaseTheme(currentTheme = Theme.LIGHT) {
        RandomModule(
            wikiSite = wikiSite,
            module = ForYouModule.Random(0, 0, mutableListOf(card, card))
        )
    }
}

@Preview
@Composable
fun RandomCardPreviewNoImage() {
    val wikiSite = WikiSite.preview()
    val title = PageTitle(
        text = "Test Article",
        displayText = "Test Article",
        wiki = WikiSite.preview(),
        description = "This is a test article",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbUrl = null
    )
    val card = RandomCard(title)
    BaseTheme(currentTheme = Theme.LIGHT) {
        RandomModule(
            wikiSite = wikiSite,
            module = ForYouModule.Random(0, 0, mutableListOf(card))
        )
    }
}

@Preview
@Composable
fun RandomCardPreviewTextOnlyWithImage() {
    val wikiSite = WikiSite.preview()
    val title = PageTitle(
        text = "Test Article",
        displayText = "Test Article",
        wiki = WikiSite.preview(),
        description = "This is a test article",
        extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        thumbUrl = "test.jpg"
    )
    val card = RandomCard(title)
    BaseTheme(currentTheme = Theme.LIGHT) {
        RandomModule(
            wikiSite = wikiSite,
            module = ForYouModule.Random(0, 0, mutableListOf(card))
        )
    }
}
