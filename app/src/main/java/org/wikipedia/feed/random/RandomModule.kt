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
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.ForYouCardContent
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.RandomCard
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme

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
    onCustomizeClick: (card: Card) -> Unit = {},
    onShuffleClick: () -> Unit = {}
) {
    val context = LocalContext.current

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
            module = module,
            card = module.cards[page],
            footerText = context.getString(wikiSite.languageCode, R.string.view_random_article_card_title),
            footerContent = {
                Button(
                    onClick = onShuffleClick,
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
            onCustomizeClick = { onCustomizeClick(card) }
        )
    }
}

@Preview
@Composable
fun RandomCardPreviewWithImage() {
    val card = RandomCard(PageTitle.preview())
    BaseTheme(currentTheme = Theme.LIGHT) {
        RandomModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.Random(0, 0, mutableListOf(card, card))
        )
    }
}

@Preview
@Composable
fun RandomCardPreviewNoImage() {
    val card = RandomCard(PageTitle.preview(withThumbnail = false))
    BaseTheme(currentTheme = Theme.LIGHT) {
        RandomModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.Random(0, 0, mutableListOf(card))
        )
    }
}

@Preview
@Composable
fun RandomCardPreviewTextOnlyWithImage() {
    val card = RandomCard(PageTitle.preview())
    BaseTheme(currentTheme = Theme.LIGHT) {
        RandomModule(
            wikiSite = WikiSite.preview(),
            module = ForYouModule.Random(0, 0, mutableListOf(card))
        )
    }
}
