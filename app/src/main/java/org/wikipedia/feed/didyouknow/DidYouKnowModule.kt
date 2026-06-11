package org.wikipedia.feed.didyouknow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.extensions.getString
import org.wikipedia.feed.CommunityModuleContainer
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil

@Composable
fun DidYouKnowModule(
    wikiSite: WikiSite,
    dyk: List<DidYouKnowItem>,
    onHideCardClick: () -> Unit = {},
    onHideModuleClick: () -> Unit,
    onPageClick: (PageTitle) -> Unit,
    onFooterClick: () -> Unit,
    onCardImpression: () -> Unit = {},
    pageOverflowContent: @Composable (Int) -> Unit = {},
    onPageOverflowClick: (PageSummary, Int) -> Unit = { _, _ -> },
) {
    val maxDidYouKnowItems = 3
    val context = LocalContext.current

    CommunityModuleContainer(
        wikiSite = wikiSite,
        titleResId = R.string.home_feed_did_you_know_title,
        subTitleResId = R.string.home_feed_did_you_know_subtitle,
        onHideCardClick = onHideCardClick,
        onHideModuleClick = onHideModuleClick,
        onCardInView = onCardImpression
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            dyk.take(maxDidYouKnowItems).forEachIndexed { index, item ->
                DidYouKnowListItem(
                    wikiSite = wikiSite,
                    dykHtml = item.html,
                    onClick = onPageClick,
                    pageOverflowContent = { pageOverflowContent(index) },
                    onPageOverflowClick = { onPageOverflowClick(it, index) }
                )
            }
        }

        TextButton(
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp),
            onClick = onFooterClick
        ) {
            Text(
                text = context.getString(wikiSite.languageCode, R.string.home_feed_did_you_know_more_label),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.progressiveColor,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                contentDescription = context.getString(wikiSite.languageCode, R.string.home_feed_did_you_know_more_label),
                tint = WikipediaTheme.colors.progressiveColor
            )
        }
    }
}

@Composable
fun DidYouKnowListItem(
    wikiSite: WikiSite,
    dykHtml: String,
    onClick: (PageTitle) -> Unit,
    pageOverflowContent: @Composable () -> Unit,
    onPageOverflowClick: (PageSummary) -> Unit = {}
) {
    var longPressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    Column {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(colorResource(R.color.blue600), colorResource(R.color.green700))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "?",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box {
                HtmlText(
                    text = StringUtil.removeBoldTags(dykHtml),
                    color = WikipediaTheme.colors.primaryColor,
                    style = MaterialTheme.typography.bodyMedium,
                    linkInteractionListener = {
                        val url = (it as LinkAnnotation.Url).url
                        onClick(PageTitle.titleForUri(url.toUri(), wikiSite))
                    },
                    onLongClickLink = { url, intOffset ->
                        longPressOffset = with(density) { DpOffset(intOffset.x.toDp(), intOffset.y.toDp()) }
                        val title = PageTitle.titleForUri(url.toUri(), wikiSite)
                        onPageOverflowClick(PageSummary(title.displayText, title.prefixedText, title.description,
                            title.extract, title.thumbUrl, title.wikiSite.languageCode))
                    }
                )
                // Zero-size anchor positioned at the tap point. DropdownMenu anchors to
                // this box so it opens exactly at the long-press location.
                Box(modifier = Modifier.offset { IntOffset(longPressOffset.x.roundToPx(), longPressOffset.y.roundToPx()) }) {
                    pageOverflowContent()
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DidYouKnowCardPreview() {
    val dykItem = DidYouKnowItem(
        "...that <a href=\"https://en.wikipedia.org/wiki/Elephant\">elephants</a> have a very long memory?",
        ""
    )
    BaseTheme(currentTheme = Theme.LIGHT) {
        DidYouKnowModule(
            wikiSite = WikiSite.preview(),
            dyk = listOf(dykItem),
            onFooterClick = {},
            onHideModuleClick = {},
            onPageClick = {},
        )
    }
}
