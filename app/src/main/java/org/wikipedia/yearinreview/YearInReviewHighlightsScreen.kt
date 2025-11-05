package org.wikipedia.yearinreview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme

@Composable
fun YearInReviewHighlightsScreen(
    modifier: Modifier = Modifier,
    screenData: YearInReviewScreenData.HighlightsScreen,
    onShareHighlightsBtnClick: () -> Unit
) {
    Column(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .padding(top = 60.dp, bottom = 16.dp),
            text = buildAnnotatedString {
                append(stringResource(R.string.year_in_review_highlights_thank_you_message))
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Normal,
                    )
                ) {
                    append(" ")
                    append(stringResource(R.string.year_in_review_highlights_looking_forward_message))
                }
            },
            color = WikipediaTheme.colors.paperColor,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        ShareableHighlightsCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .background(ComposeColors.Gray100)
                .border(width = 1.dp, color = ComposeColors.Gray300)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
            highlights = screenData.highlights
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor
            ),
            onClick = onShareHighlightsBtnClick
        ) {
            Text(
                text = stringResource(R.string.year_in_review_highlights_share_button_title),
                color = WikipediaTheme.colors.paperColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ShareableHighlightsCard(
    modifier: Modifier = Modifier,
    hashtag: String = stringResource(R.string.year_in_review_hashtag),
    logoResource: Int = R.drawable.w_nav_mark,
    logoDescription: String = stringResource(R.string.year_in_review_highlights_logo_description),
    highlights: List<YearInReviewScreenData.HighlightItem>,
) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = hashtag,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                color = ComposeColors.Gray700
            )
            Image(
                modifier = Modifier
                    .size(163.dp)
                    .padding(vertical = 4.dp),
                painter = painterResource(logoResource),
                contentDescription = logoDescription
            )
            Text(
                modifier = Modifier
                    .padding(4.dp),
                text = logoDescription,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = ComposeColors.Gray700
            )
            highlights.forEachIndexed { index, highlightItem ->
                HighlightsContent(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = if (index == highlights.size - 1) 4.dp else 0.dp),
                    highlightItem = highlightItem
                )
            }
        }

        if (scrollState.maxValue > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(8.dp)
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                val scrollbarHeight = (scrollState.viewportSize.toFloat()) / (scrollState.maxValue + scrollState.viewportSize) * 0.5f
                val scrollbarOffsetY = (scrollState.value.toFloat() / scrollState.maxValue) * (1f - scrollbarHeight)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(scrollbarHeight)
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = ((scrollState.viewportSize) * scrollbarOffsetY).toInt()
                            )
                        }
                        .background(
                            color = ComposeColors.Gray300,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun HighlightsContent(
    modifier: Modifier = Modifier,
    highlightItem: YearInReviewScreenData.HighlightItem
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier
                .width(98.dp),
            text = highlightItem.title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        if (highlightItem.singleValue != null) {
            Text(
                text = highlightItem.singleValue,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Color.Black
            )
        } else {
            Column {
                highlightItem.items.fastForEachIndexed { index, item ->
                    Text(
                        text = buildAnnotatedString {
                            append("${index + 1}. ")
                            withStyle(
                                style = SpanStyle(
                                    color = highlightItem.highlightColor
                                )
                            ) {
                                append(text = item)
                            }
                        },
                        lineHeight = 21.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ShareHighlightsScreenCapture(
    highlights: List<YearInReviewScreenData.HighlightItem>,
    onBitmapReady: (Bitmap) -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    var isReadyToCapture by remember { mutableStateOf(false) }

    if (isReadyToCapture) {
        LaunchedEffect(Unit) {
            val bitmap = graphicsLayer.toImageBitmap()
            onBitmapReady(bitmap.asAndroidBitmap())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                isReadyToCapture = true
            }
            .drawWithContent {
                graphicsLayer.record {
                    drawRect(
                        color = ComposeColors.White
                    )
                    this@drawWithContent.drawContent()
                }
            }
            .background(ComposeColors.White),
    ) {
        ShareableHighlightsCard(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 34.dp)
                .background(ComposeColors.Gray100)
                .border(width = 1.dp, color = ComposeColors.Gray300)
                .padding(8.dp),
            highlights = highlights
        )

        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            text = stringResource(R.string.year_in_highlights_screenshot_url),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(device = Devices.PIXEL_9)
@Composable
private fun YearInReviewHighlightsScreenPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        YearInReviewHighlightsScreen(
            screenData = YearInReviewScreenData.HighlightsScreen(
                highlights = listOf(
                    YearInReviewScreenData.HighlightItem(
                        title = "Articles I read the longest",
                        items = listOf(
                            "Pamela Anderson",
                            "Pamukkale",
                            "History of US science fiction and fantasy magazines to 1950"
                        ),
                        highlightColor = ComposeColors.Blue600
                    )
                )
            ),
            onShareHighlightsBtnClick = {}
        )
    }
}

@Preview
@Composable
private fun ListItemsPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HighlightsContent(
            highlightItem = YearInReviewScreenData.HighlightItem(
                title = "Articles I read the longest",
                items = listOf(
                    "Pamela Anderson",
                    "Pamukkale",
                    "History of US science fiction and fantasy magazines to 1950"
                ),
                highlightColor = ComposeColors.Blue600
            )
        )
    }
}

@Preview
@Composable
private fun SingleValuePreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        HighlightsContent(
            highlightItem = YearInReviewScreenData.HighlightItem(
                title = "Minutes read",
                singleValue = "924",
                highlightColor = ComposeColors.Blue600
            )
        )
    }
}
