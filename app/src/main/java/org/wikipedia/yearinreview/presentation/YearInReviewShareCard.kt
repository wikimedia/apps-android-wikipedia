package org.wikipedia.yearinreview.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import java.text.NumberFormat

private const val SHARE_CARD_WIDTH_PX = 1080

private val ShareCardWidth = 360.dp
private val ShareCardMinHeight = 360.dp
private val ShareCardHorizontalPadding = 12.dp
private val ShareCardLabelColumnWidth = 162.dp
private val ShareCardLabelEndPadding = 32.dp
private val ShareCardListNumberWidth = 22.dp

private val ShareCardBackgroundColor = Color(0xFFE1F0E1)
private val ShareCardContentColor = ComposeColors.Black

private val SerifFontFamily = FontFamily(Font(R.font.ebgaramond_regular))
private val SansSerifFontFamily = FontFamily(
    Font(R.font.robotoflex_regular),
    Font(R.font.robotoflex_bold, FontWeight.Bold)
)

private val TitleTextStyle = TextStyle(
    color = ShareCardContentColor,
    fontFamily = SerifFontFamily,
    fontSize = 43.sp,
    lineHeight = 43.sp
)
private val LabelTextStyle = TextStyle(
    color = ShareCardContentColor,
    fontFamily = SerifFontFamily,
    fontSize = 16.sp,
    lineHeight = 16.sp
)
private val NumberTextStyle = TextStyle(
    color = ShareCardContentColor,
    fontFamily = SansSerifFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 27.sp
)
private val ValueTextStyle = TextStyle(
    color = ShareCardContentColor,
    fontFamily = SansSerifFontFamily,
    fontSize = 13.5.sp,
    lineHeight = 14.5.sp
)
private val ListNumberTextStyle = TextStyle(
    color = ShareCardContentColor,
    fontFamily = SerifFontFamily,
    fontSize = 9.sp,
    letterSpacing = 2.sp,
    fontWeight = FontWeight.Bold,
    // Centers the number on the first line of the item next to it
    lineHeight = ValueTextStyle.lineHeight,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
)

@Composable
fun YearInReviewShareCard(
    year: Int,
    readingStats: YearInReviewReadingStats,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    val numberFormat = remember(locale) { NumberFormat.getNumberInstance(locale) }
    Column(
        modifier = modifier
            .width(ShareCardWidth)
            .heightIn(min = ShareCardMinHeight)
            .background(ShareCardBackgroundColor)
            .padding(horizontal = ShareCardHorizontalPadding),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                modifier = Modifier
                    .padding(top = 6.dp),
                    // .widthIn(max = 244.dp), // in case we want to further constrain the headline text
                text = stringResource(R.string.yir_feature_card_title, year),
                style = TitleTextStyle
            )
            Image(
                modifier = Modifier
                    .align(Alignment.End)
                    .width(250.dp),
                painter = painterResource(R.drawable.crossword_sharecard),
                contentDescription = null
            )
            ShareCardDivider()
            Row(
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp)
            ) {
                ShareCardNumberStat(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.yir_feature_card_articles_read),
                    value = numberFormat.format(readingStats.articlesReadCount)
                )
                val minutesRead = readingStats.totalTimeSpentSec / 60
                if (minutesRead > 0) {
                    ShareCardNumberStat(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.yir_feature_card_minutes_read),
                        value = numberFormat.format(minutesRead),
                        horizontalAlignment = Alignment.End
                    )
                }
            }
            if (readingStats.articlesRevisited.size > 1) {
                ShareCardDivider()
                ShareCardListStat(label = stringResource(R.string.yir_feature_card_articles_reread)) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        readingStats.articlesRevisited.forEachIndexed { index, (title, _) ->
                            Row {
                                Text(
                                    modifier = Modifier.width(ShareCardListNumberWidth),
                                    text = "(${numberFormat.format(index + 1)})",
                                    style = ListNumberTextStyle
                                )
                                Text(
                                    text = StringUtil.fromHtml(title.displayText).toString(),
                                    style = ValueTextStyle
                                )
                            }
                        }
                    }
                }
            }
            if (readingStats.rabbitHoleStart != null && readingStats.rabbitHoleEnd != null) {
                ShareCardDivider()
                ShareCardListStat(label = stringResource(R.string.yir_feature_card_longest_rabbit_hole)) {
                    Column {
                        Text(
                            text = StringUtil.fromHtml(readingStats.rabbitHoleStart.displayText).toString(),
                            style = ValueTextStyle
                        )
                        Text(
                            text = "↓ " + StringUtil.fromHtml(readingStats.rabbitHoleEnd.displayText),
                            style = ValueTextStyle
                        )
                    }
                }
            }
            if (!readingStats.topCategory.isNullOrEmpty()) {
                ShareCardDivider()
                ShareCardListStat(label = stringResource(R.string.yir_feature_card_top_category)) {
                    Text(
                        text = StringUtil.removeNamespace(readingStats.topCategory),
                        style = ValueTextStyle
                    )
                }
            }
        }
        Column {
            ShareCardDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.yir_feature_card_created_with),
                    style = LabelTextStyle
                )
                Icon(
                    modifier = Modifier.width(26.dp),
                    painter = painterResource(R.drawable.ic_wikipedia_w),
                    tint = ShareCardContentColor,
                    contentDescription = null
                )
            }
        }
    }
}

// Draws the card off-screen at a fixed pixel width, so the image doesn't depend on the device's density or font scale.
@Composable
fun YearInReviewShareCardRenderer(
    year: Int,
    readingStats: YearInReviewReadingStats,
    onRendered: (Bitmap) -> Unit,
    onError: (Throwable) -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    val recorded = remember { CompletableDeferred<Unit>() }
    val currentOnRendered by rememberUpdatedState(onRendered)
    val currentOnError by rememberUpdatedState(onError)
    CompositionLocalProvider(
        LocalDensity provides Density(density = SHARE_CARD_WIDTH_PX / ShareCardWidth.value, fontScale = 1f)
    ) {
        YearInReviewShareCard(
            modifier = Modifier
                // Takes up no space, so that it doesn't affect the layout around it
                .layout { measurable, _ ->
                    val placeable = measurable.measure(Constraints())
                    layout(0, 0) {
                        placeable.place(0, 0)
                    }
                }
                // Records the card without drawing it on screen
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    recorded.complete(Unit)
                },
            year = year,
            readingStats = readingStats
        )
    }
    LaunchedEffect(Unit) {
        recorded.await()
        try {
            // The layer's bitmap may be a hardware bitmap, which can't be shared directly
            currentOnRendered(graphicsLayer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false))
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            currentOnError(exception)
        }
    }
}

@Composable
private fun ShareCardDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val dashLength = 2.dp.toPx()
        drawLine(
            color = ShareCardContentColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength))
        )
    }
}

@Composable
private fun ShareCardNumberStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    val textAlign = if (horizontalAlignment == Alignment.End) TextAlign.End else TextAlign.Start
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            style = LabelTextStyle.copy(textAlign = textAlign)
        )
        Text(
            text = value,
            style = NumberTextStyle.copy(textAlign = textAlign)
        )
    }
}

@Composable
private fun ShareCardListStat(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)
    ) {
        Text(
            modifier = Modifier
                .width(ShareCardLabelColumnWidth)
                .padding(end = ShareCardLabelEndPadding),
            text = label,
            style = LabelTextStyle
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun YearInReviewShareCardPreview() {
    val wiki = WikiSite.preview()
    YearInReviewShareCard(
        year = 2026,
        readingStats = YearInReviewReadingStats(
            articlesReadCount = 1253938,
            visitedDaysCount = 200,
            totalTimeSpentSec = 1253937L * 60,
            peakMonth = 12,
            peakMonthVisitedDays = 20,
            articlesRevisited = listOf(
                PageTitle("Pamela Anderson", wiki) to 5,
                PageTitle("Pamukkale", wiki) to 4,
                PageTitle("History of US science fiction and fantasy magazines to 1950", wiki) to 3
            ),
            topCategory = "Category:Executed female serial killers",
            rabbitHoleStart = PageTitle("Frida Kahlo", wiki),
            rabbitHoleEnd = PageTitle("Diego Rivera", wiki),
            rabbitHoleFakeStart = PageTitle("Moon", wiki),
            rabbitHoleFakeEnd = PageTitle("Sun", wiki),
            rabbitHoleArticleCount = 5
        )
    )
}
