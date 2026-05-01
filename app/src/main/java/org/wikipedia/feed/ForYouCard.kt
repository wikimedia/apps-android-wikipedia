package org.wikipedia.feed

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
import org.wikipedia.feed.model.Card
import org.wikipedia.history.HistoryEntry
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
    entry: HistoryEntry,
    variation: CardVariation = CardVariation.VARIATION_IMAGE_WITH_EXTRACT,
    backgroundColorIndex: Int = 0,
    module: ForYouModule? = null,
    card: Card? = null,
    footerIcon: Painter? = null,
    footerText: String? = null,
    onPageClick: (HistoryEntry) -> Unit = {},
    onHideCardClick: (module: ForYouModule, card: Card) -> Unit = { _, _ -> },
    onHideModuleClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onPageClick(HistoryEntry(entry.title, HistoryEntry.SOURCE_FEED_CONTINUE_READING)) }
    ) {
        if (entry.title.thumbUrl.isNullOrEmpty() || variation == CardVariation.VARIATION_TEXT_ONLY) {
            val color = colorResource(noImageCardBackgroundColors[backgroundColorIndex % noImageCardBackgroundColors.size])
            Box(
                modifier = Modifier.fillMaxSize().background(color)
            )
        } else {
            FadeInAsyncImage(
                model = entry.title.thumbUrl?.let { ImageService.getRequest(LocalContext.current,
                    url = ImageUrlUtil.getUrlForPreferredSize(it, Constants.PREFERRED_CARD_THUMBNAIL_SIZE)) },
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.DarkGray),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (entry.title.thumbUrl.isNullOrEmpty() || variation == CardVariation.VARIATION_TEXT_ONLY) {
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
                    val text = entry.title.extract ?: entry.title.description ?: ""
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
                        if (!entry.title.thumbUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = entry.title.thumbUrl,
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
                                text = entry.title.displayText,
                                color = colorResource(R.color.gray700),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            HtmlText(
                                modifier = Modifier.padding(top = 4.dp),
                                text = entry.title.description ?: "",
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
                                onHideCardClick = {
                                    if (module != null && card != null) {
                                        onHideCardClick(module, card)
                                    }
                                },
                                onHideModuleClick = onHideModuleClick
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
                            contentDescription = context.getString(
                                wikiSite.languageCode,
                                R.string.menu_feed_overflow_label
                            ),
                            tint = Color.White,
                            modifier = Modifier.padding(end = 4.dp).size(16.dp)
                        )
                    }
                    footerText?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.background(
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
                ) {
                    Spacer(modifier = Modifier.height(100.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HtmlText(
                            modifier = Modifier.weight(1f).padding(start = 16.dp, top = 8.dp),
                            text = entry.title.displayText,
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
                            onHideCardClick = {
                                if (module != null && card != null) {
                                    onHideCardClick(module, card)
                                }
                            },
                            onHideModuleClick = onHideModuleClick
                        )
                    }
                }
                Column(
                    modifier = Modifier.background(color = Color.Black.copy(alpha = 0.80f))
                        .padding(bottom = 40.dp)
                ) {
                    val text = if (variation == CardVariation.VARIATION_IMAGE_WITH_EXTRACT) entry.title.extract else entry.title.description
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
                                contentDescription = context.getString(
                                    wikiSite.languageCode,
                                    R.string.menu_feed_overflow_label
                                ),
                                tint = Color.White,
                                modifier = Modifier.padding(end = 4.dp).size(16.dp)
                            )
                        }
                        footerText?.let {
                            Text(
                                text = it,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 8,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit = {}
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
    }
}

@Preview
@Composable
fun ForYouModulePreviewWithImage() {
    val wikiSite = WikiSite.preview()
    val entry = HistoryEntry(
        title = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite.preview(),
            description = "This is a test article",
            extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            thumbUrl = "https://example.com/thumb.jpg"
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouCardContent(
            wikiSite = wikiSite,
            entry = entry,
            footerText = "Lorem ipsum"
        )
    }
}

@Preview
@Composable
fun ForYouModulePreviewNoImage() {
    val wikiSite = WikiSite.preview()
    val entry = HistoryEntry(
        title = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite.preview(),
            description = "This is a test article",
            extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            thumbUrl = null
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouCardContent(
            wikiSite = wikiSite,
            entry = entry,
            footerText = "Lorem ipsum"
        )
    }
}

@Preview
@Composable
fun ForYouModulePreviewTextOnlyWithImage() {
    val wikiSite = WikiSite.preview()
    val entry = HistoryEntry(
        title = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite.preview(),
            description = "This is a test article",
            extract = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            thumbUrl = "test.jpg"
        ), source = HistoryEntry.SOURCE_HISTORY
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        ForYouCardContent(
            wikiSite = wikiSite,
            entry = entry,
            footerText = "Lorem ipsum",
            variation = CardVariation.VARIATION_TEXT_ONLY,
            backgroundColorIndex = 2
        )
    }
}
