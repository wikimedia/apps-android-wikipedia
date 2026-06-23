package org.wikipedia.feed

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.compose.components.FadeInAsyncImage
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.feed.model.ForYouCard
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.ImageUrlUtil
import org.wikipedia.views.imageservice.ImageService

enum class CardVariation {
    VARIATION_IMAGE_WITH_EXTRACT,
    VARIATION_IMAGE_WITH_DESCRIPTION,
    VARIATION_TEXT_ONLY
}

@Composable
fun ForYouCardContent(
    wikiSite: WikiSite,
    title: PageTitle,
    variation: CardVariation = CardVariation.VARIATION_IMAGE_WITH_EXTRACT,
    backgroundColorIndex: Int = 0,
    module: ForYouModule? = null,
    card: ForYouCard? = null,
    footerIcon: Painter? = null,
    footerText: String? = null,
    footerContent: @Composable () -> Unit = {},
    onPageClick: (PageTitle) -> Unit = {},
    onShareClick: (PageTitle) -> Unit = {},
    onSaveClick: (PageTitle) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: ForYouCard) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {},
    @StringRes customizeMenuText: Int = R.string.explore_feed_customize_interests,
    onCustomizeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    val showSpaceForPagerDots = (module?.cards?.size ?: 0) > 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onPageClick(title) }
    ) {
        val shouldShowTextOnly = title.thumbUrl.isNullOrEmpty() || variation == CardVariation.VARIATION_TEXT_ONLY
        if (shouldShowTextOnly) {
            val color = colorResource(noImageCardBackgroundColors[backgroundColorIndex % noImageCardBackgroundColors.size])
            Box(
                modifier = Modifier.fillMaxSize().background(color)
            )
        } else {
            FadeInAsyncImage(
                model = title.thumbUrl?.let { ImageService.getRequest(context,
                    url = ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)) },
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.DarkGray),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (shouldShowTextOnly) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorResource(noImageCardForegroundColors[backgroundColorIndex % noImageCardForegroundColors.size]))
                        .padding(16.dp)
                ) {
                    val text = title.extract ?: title.description ?: ""
                    HtmlText(
                        text = text,
                        color = colorResource(R.color.gray700),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 8
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp).width(48.dp),
                        thickness = 1.dp,
                        color = colorResource(noImageCardBackgroundColors[backgroundColorIndex % noImageCardBackgroundColors.size])
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!title.thumbUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = title.thumbUrl,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp).size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            HtmlText(
                                text = title.displayText,
                                color = colorResource(R.color.gray700),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            HtmlText(
                                modifier = Modifier.padding(top = 4.dp),
                                text = title.description ?: "",
                                color = colorResource(R.color.gray700),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                        Column {
                            IconButton(
                                modifier = Modifier.size(48.dp),
                                onClick = {
                                    overflowMenuExpanded = true
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                                    contentDescription = context.getString(
                                        wikiSite.languageCode,
                                        R.string.menu_feed_overflow_label
                                    ),
                                    tint = colorResource(R.color.gray700),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            ForYouCardDropdownMenu(
                                expanded = overflowMenuExpanded,
                                wikiSite = wikiSite,
                                onDismiss = { overflowMenuExpanded = false },
                                onShareClick = {
                                    onShareClick(title)
                                },
                                onSaveClick = {
                                    onSaveClick(title)
                                },
                                onHideCardClick = {
                                    if (module != null && card != null) {
                                        onHideCardClick(module, card)
                                    }
                                },
                                onHideModuleClick = onHideModuleClick,
                                customizeMenuText = customizeMenuText,
                                onCustomizeClick = onCustomizeClick
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    footerIcon?.let {
                        Icon(
                            painter = it,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 4.dp).size(16.dp)
                        )
                    }
                    footerText?.let {
                        HtmlText(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                footerContent()
                Spacer(modifier = Modifier.height(if (showSpaceForPagerDots) 40.dp else 16.dp))
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                        .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.18f to Color.Black.copy(alpha = 0.05f),
                                0.38f to Color.Black.copy(alpha = 0.15f),
                                0.58f to Color.Black.copy(alpha = 0.30f),
                                0.76f to Color.Black.copy(alpha = 0.50f),
                                0.90f to Color.Black.copy(alpha = 0.7f),
                                1.0f to Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
                )
                Column(
                    modifier = Modifier.background(color = Color.Black.copy(alpha = 0.80f))
                        .padding(bottom = if (showSpaceForPagerDots) 40.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HtmlText(
                            modifier = Modifier.weight(1f).padding(start = 16.dp, top = 8.dp),
                            text = title.displayText,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif
                            ),
                            maxLines = 3
                        )
                        IconButton(
                            modifier = Modifier.size(48.dp),
                            onClick = {
                                overflowMenuExpanded = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert_white_24dp),
                                contentDescription = context.getString(
                                    wikiSite.languageCode,
                                    R.string.menu_feed_overflow_label
                                ),
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        ForYouCardDropdownMenu(
                            expanded = overflowMenuExpanded,
                            wikiSite = wikiSite,
                            onDismiss = { overflowMenuExpanded = false },
                            onShareClick = {
                                onShareClick(title)
                            },
                            onSaveClick = {
                                onSaveClick(title)
                            },
                            onHideCardClick = {
                                if (module != null && card != null) {
                                    onHideCardClick(module, card)
                                }
                            },
                            onHideModuleClick = onHideModuleClick,
                            customizeMenuText = customizeMenuText,
                            onCustomizeClick = onCustomizeClick
                        )
                    }
                    val text = if (variation == CardVariation.VARIATION_IMAGE_WITH_EXTRACT) (title.extract ?: title.description) else title.description
                    text?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        HtmlText(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        footerIcon?.let {
                            Icon(
                                painter = it,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(end = 4.dp).size(16.dp)
                            )
                        }
                        footerText?.let {
                            HtmlText(
                                text = it,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    footerContent()
                }
            }
        }
    }
}

@Composable
fun ForYouCardDropdownMenu(
    expanded: Boolean,
    wikiSite: WikiSite,
    onDismiss: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {},
    @StringRes customizeMenuText: Int = R.string.explore_feed_customize_interests,
    onCustomizeClick: () -> Unit
) {
    val context = LocalContext.current
    DropdownMenu(
        offset = DpOffset(x = (-16).dp, y = 0.dp),
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = WikipediaTheme.colors.paperColor
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, R.string.menu_page_article_share),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onShareClick()
                onDismiss()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark_border_white_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, R.string.menu_page_add_to_default_list),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onSaveClick()
                onDismiss()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_no_sim_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, R.string.menu_feed_card_dismiss),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onHideCardClick()
                onDismiss()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_visibility_off_24dp),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, R.string.explore_feed_header_overflow_hide_module_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onHideModuleClick()
                onDismiss()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_tune_24),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.secondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            },
            text = {
                Text(
                    text = context.getString(wikiSite.languageCode, customizeMenuText),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WikipediaTheme.colors.primaryColor
                )
            },
            onClick = {
                onCustomizeClick()
                onDismiss()
            }
        )
    }
}

@Preview
@Composable
fun ForYouModulePreviewWithImage() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouCardContent(
            wikiSite = WikiSite.preview(),
            title = PageTitle.preview(),
            footerText = "Lorem ipsum"
        )
    }
}

@Preview
@Composable
fun ForYouModulePreviewNoImage() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouCardContent(
            wikiSite = WikiSite.preview(),
            title = PageTitle.preview(withThumbnail = false),
            footerText = "Lorem ipsum"
        )
    }
}

@Preview
@Composable
fun ForYouModulePreviewTextOnlyWithImage() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouCardContent(
            wikiSite = WikiSite.preview(),
            title = PageTitle.preview(),
            footerText = "Lorem ipsum",
            variation = CardVariation.VARIATION_TEXT_ONLY,
            backgroundColorIndex = 2
        )
    }
}
