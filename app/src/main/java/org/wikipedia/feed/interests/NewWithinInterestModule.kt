package org.wikipedia.feed.interests

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.Service
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.ForYouCardDropdownMenu
import org.wikipedia.feed.ForYouModule
import org.wikipedia.feed.ForYouModulePager
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.feed.model.NewWithinInterestCard
import org.wikipedia.feed.noImageCardBackgroundColors
import org.wikipedia.feed.personalization.db.entity.InterestTopic
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.topics.ArticleTopics
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.views.imageservice.ImageService
import kotlin.math.abs

private const val MAX_ARTICLES_PER_CARD = 4

@Composable
fun NewWithinInterestModule(
    modifier: Modifier = Modifier,
    topInset: Int,
    wikiSite: WikiSite,
    module: ForYouModule.NewWithinInterest,
    onPageClick: (card: Card, historyEntry: HistoryEntry) -> Unit = { _, _ -> },
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    onCardInView: (card: Card) -> Unit = {},
    onCustomizeClick: (card: Card) -> Unit = {},
) {
    val context = LocalContext.current
    val backgroundColorIndex = abs(module.cards.firstOrNull()?.hideKey.hashCode())

    ForYouModulePager(
        modifier = modifier,
        module = module,
        onCardInView = onCardInView
    ) { pageIndex ->
        val card = module.cards[pageIndex] as NewWithinInterestCard
        val topic = ArticleTopics.all.find { it.topicId == card.interestTopic.topicId }
        val topicName = topic?.let { context.getString(wikiSite.languageCode, it.msgKey) } ?: card.interestTopic.topicId

        NewWithinInterestCardContent(
            wikiSite = wikiSite,
            titles = card.titles.take(MAX_ARTICLES_PER_CARD),
            title = context.getString(wikiSite.languageCode, R.string.home_feed_new_within_interest_title, topicName),
            backgroundColor = colorResource(noImageCardBackgroundColors[(backgroundColorIndex + pageIndex) % noImageCardBackgroundColors.size]),
            topInset = topInset,
            bottomSpacing = if (module.cards.size > 1) 40.dp else 16.dp,
            onPageClick = { onPageClick(card, HistoryEntry(it, HistoryEntry.SOURCE_FEED_INTERESTS)) },
            onHideCardClick = { onHideCardClick(module, card) },
            onHideModuleClick = onHideModuleClick,
            onCustomizeClick = { onCustomizeClick(card) }
        )
    }
}

@Composable
private fun NewWithinInterestCardContent(
    wikiSite: WikiSite,
    titles: List<PageTitle>,
    title: String,
    backgroundColor: Color,
    topInset: Int,
    bottomSpacing: Dp,
    onPageClick: (PageTitle) -> Unit = {},
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {},
    onCustomizeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 16.dp)
            .padding(top = (topInset * 2 + 64).dp, bottom = bottomSpacing),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                IconButton(onClick = { overflowMenuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                        contentDescription = context.getString(wikiSite.languageCode, R.string.menu_feed_overflow_label),
                        tint = WikipediaTheme.colors.primaryColor,
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
                    onCustomizeClick = onCustomizeClick
                )
            }
        }

        titles.forEach { pageTitle ->
            NewWithinInterestArticleCard(
                modifier = Modifier.weight(1f),
                title = pageTitle,
                onClick = { onPageClick(pageTitle) }
            )
        }
    }
}

@Composable
private fun NewWithinInterestArticleCard(
    modifier: Modifier = Modifier,
    title: PageTitle,
    onClick: () -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, WikipediaTheme.colors.primaryColor),
        colors = CardDefaults.cardColors(containerColor = WikipediaTheme.colors.backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HtmlText(
                    text = title.displayText,
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                title.description?.let {
                    HtmlText(
                        text = it,
                        color = WikipediaTheme.colors.primaryColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            title.thumbUrl?.let { thumbUrl ->
                AsyncImage(
                    model = ImageService.getRequest(LocalContext.current,
                        url = ImageUrlUtil.getUrlForPreferredSize(thumbUrl, Service.PREFERRED_THUMB_SIZE)),
                    error = BrushPainter(SolidColor(WikipediaTheme.colors.placeholderColor)),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Preview
@Composable
private fun NewWithinInterestModulePreview() {
    val card = NewWithinInterestCard(
        titles = List(MAX_ARTICLES_PER_CARD) { PageTitle.preview() },
        interestTopic = InterestTopic("sports")
    )
    BaseTheme(currentTheme = Theme.BLACK) {
        NewWithinInterestModule(
            modifier = Modifier.fillMaxSize(),
            topInset = 0,
            wikiSite = WikiSite.preview(),
            module = ForYouModule.NewWithinInterest(0, 0, listOf(card, card))
        )
    }
}

@Preview
@Composable
private fun NewWithinInterestModulePreviewNoImage() {
    val card = NewWithinInterestCard(
        titles = List(MAX_ARTICLES_PER_CARD) { PageTitle.preview(withThumbnail = false) },
        interestTopic = InterestTopic("sports")
    )
    BaseTheme(currentTheme = Theme.BLACK) {
        NewWithinInterestModule(
            modifier = Modifier.fillMaxSize(),
            topInset = 0,
            wikiSite = WikiSite.preview(),
            module = ForYouModule.NewWithinInterest(0, 0, listOf(card))
        )
    }
}
