package org.wikipedia.feed.image

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.FadeInAsyncImage
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CommunityModuleContainer
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

@Composable
fun FeaturedImageModule(
    wikiSite: WikiSite,
    card: FeaturedImageCard,
    onClick: (card: FeaturedImageCard) -> Unit = {},
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: (featuredImage: FeaturedImage) -> Unit = {},
    onDownloadClick: (card: FeaturedImageCard) -> Unit = {},
    onShareClick: (card: FeaturedImageCard) -> Unit = {},
    onCardImpression: () -> Unit = {}
) {
    val context = LocalContext.current
    val featuredImage = card.featuredImage

    CommunityModuleContainer(
        wikiSite = wikiSite,
        titleResId = R.string.view_featured_image_card_title,
        subTitleResId = R.string.explore_feed_potd_subtitle,
        contextIconResId = R.drawable.ic_commons_logo,
        onHideCardClick = onHideCardClick,
        onHideModuleClick = { onHideModuleClick(featuredImage) },
        onCardInView = onCardImpression
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth().padding(bottom = 24.dp)
                .clickable {
                    onClick(card)
                }
        ) {
            FadeInAsyncImage(
                model = ImageService.getRequest(context, url = featuredImage.thumbnailUrl),
                placeholder = ColorPainter(WikipediaTheme.colors.backgroundColor),
                error = ColorPainter(WikipediaTheme.colors.backgroundColor),
                contentDescription = featuredImage.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(
                    onClick = { onDownloadClick(card) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = WikipediaTheme.colors.backgroundColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download_24px),
                            contentDescription = context.getString(wikiSite.languageCode, R.string.view_featured_image_card_download),
                            tint = WikipediaTheme.colors.primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                IconButton(
                    onClick = { onShareClick(card) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = WikipediaTheme.colors.backgroundColor,
                                shape = CircleShape
                            ),
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.18f to Color.Black.copy(alpha = 0.05f),
                            0.38f to Color.Black.copy(alpha = 0.15f),
                            0.58f to Color.Black.copy(alpha = 0.30f),
                            0.76f to Color.Black.copy(alpha = 0.50f),
                            0.90f to Color.Black.copy(alpha = 0.7f),
                            1.0f to Color.Black.copy(alpha = 0.85f)
                        )
                    ))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 80.dp, bottom = 16.dp)
                ) {
                    HtmlText(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = featuredImage.description.text,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        linkStyle = TextLinkStyles(
                            style = SpanStyle(
                                color = Color.White,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_user_avatar),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp).padding(2.dp)
                        )
                        HtmlText(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            text = "${featuredImage.artist?.html.orEmpty()} - ${featuredImage.credit?.html.orEmpty()}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            linkStyle = TextLinkStyles(
                                style = SpanStyle(
                                    color = Color.White,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(featuredImage.license?.licenseIcon() ?: R.drawable.ic_info_outline_black_24dp),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        HtmlText(
                            modifier = Modifier.weight(1f).padding(start = 4.dp),
                            text = "<a href=\"${featuredImage.license?.licenseUrl.orEmpty()}\">${featuredImage.license?.licenseName.orEmpty()}</a>",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            linkStyle = TextLinkStyles(
                                style = SpanStyle(
                                    color = Color.White,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedImageCardPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        FeaturedImageModule(
            wikiSite = WikiSite.preview(),
            FeaturedImageCard(FeaturedImage("Lorem ipsum"), 0, WikiSite.preview())
        )
    }
}
