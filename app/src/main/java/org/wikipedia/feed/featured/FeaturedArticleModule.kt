package org.wikipedia.feed.featured

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.FadeInAsyncImage
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CommunityModuleContainer
import org.wikipedia.feed.noImageCardBackgroundColors
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService
import kotlin.random.Random

@Composable
fun FeaturedArticleModule(
    wikiSite: WikiSite,
    article: PageSummary,
    onPageClick: (article: PageSummary) -> Unit = {},
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {},
    onShareClick: (article: PageSummary) -> Unit = {},
    onBookmarkClick: (article: PageSummary) -> Unit = {},
    onCardImpression: () -> Unit = {}
) {
    val context = LocalContext.current

    CommunityModuleContainer(
        wikiSite = wikiSite,
        titleResId = R.string.view_featured_article_card_title,
        subTitleResId = R.string.explore_feed_featured_article_subtitle,
        onHideCardClick = onHideCardClick,
        onHideModuleClick = onHideModuleClick,
        onCardInView = onCardImpression
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onPageClick(article) }
        ) {
            if (article.thumbnailUrl.isNullOrEmpty()) {
                val color = colorResource(noImageCardBackgroundColors.random(Random(article.apiTitle.hashCode())))
                Box(
                    modifier = Modifier.fillMaxWidth().height(415.dp).background(color)
                )
            } else {
                FadeInAsyncImage(
                    model = article.thumbnailUrl?.let { ImageService.getRequest(context, url = it) },
                    placeholder = ColorPainter(WikipediaTheme.colors.backgroundColor),
                    error = ColorPainter(WikipediaTheme.colors.backgroundColor),
                    contentDescription = article.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(415.dp)
                )
                Box(
                    modifier = Modifier.fillMaxWidth().height(415.dp).background(Color(0, 0, 0, 100))
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = { onBookmarkClick(article) }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = WikipediaTheme.colors.backgroundColor,
                                shape = CircleShape
                            )
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark_border_white_24dp),
                            contentDescription = context.getString(wikiSite.languageCode, R.string.feed_card_add_to_default_list),
                            tint = WikipediaTheme.colors.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = { onShareClick(article) }
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = WikipediaTheme.colors.backgroundColor,
                                shape = CircleShape
                            )
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = context.getString(wikiSite.languageCode, R.string.view_featured_image_card_share),
                            tint = WikipediaTheme.colors.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (article.thumbnailUrl.isNullOrEmpty()) Color.Transparent else WikipediaTheme.colors.paperColor.copy(alpha = 0.92f))
                    .padding(16.dp)
            ) {
                HtmlText(
                    text = article.displayTitle,
                    color = if (article.thumbnailUrl.isNullOrEmpty()) Color.White else WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif
                    ),
                    maxLines = 3
                )
                article.description?.let { description ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        color = if (article.thumbnailUrl.isNullOrEmpty()) Color.White.copy(alpha = 0.8f) else WikipediaTheme.colors.secondaryColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                article.extract?.let { extract ->
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp).width(48.dp),
                        thickness = 1.dp,
                        color = if (article.thumbnailUrl.isNullOrEmpty()) Color.White.copy(alpha = 0.8f) else WikipediaTheme.colors.secondaryColor.copy(alpha = 0.2f)
                    )
                    Text(
                        text = extract,
                        color = if (article.thumbnailUrl.isNullOrEmpty()) Color.White else WikipediaTheme.colors.primaryColor,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (article.thumbnailUrl.isNullOrEmpty()) 6 else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedArticleCardWithImagePreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        FeaturedArticleModule(
            wikiSite = WikiSite.preview(),
            article = PageSummary.preview()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedArticleCardNoImagePreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        FeaturedArticleModule(
            wikiSite = WikiSite.preview(),
            article = PageSummary.preview(withThumbnail = false)
        )
    }
}
