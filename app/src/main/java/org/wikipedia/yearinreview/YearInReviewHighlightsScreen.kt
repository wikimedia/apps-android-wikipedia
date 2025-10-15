package org.wikipedia.yearinreview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
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
    screenData: YearInReviewScreenData.HighlightsScreen
) {
    Column(
        modifier = modifier
            .yearInReviewHeaderBackground()
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = 60.dp),
            text = buildAnnotatedString {
                append(stringResource(R.string.year_in_review_highlights_thank_you_message))
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Normal,
                    )
                ) {
                    append(stringResource(R.string.year_in_review_highlights_looking_forward_message))
                }
            },
            color = WikipediaTheme.colors.paperColor,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(324.dp)
                .background(ComposeColors.Gray100)
                .border(width = 1.dp, color = ComposeColors.Gray300)
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "#WikipediaYearinReview",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 21.sp,
                color = ComposeColors.Gray700
            )
            Image(
                modifier = Modifier
                    .size(163.dp)
                    .padding(vertical = 4.dp),
                painter = painterResource(R.drawable.w_nav_mark),
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .padding(4.dp),
                text = "Wikipedia logo",
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = ComposeColors.Gray700
            )
            screenData.highlights.forEach { highlightItem ->
                HighlightsContent(
                    modifier = Modifier
                        .padding(top = 12.dp),
                    highlightItem = highlightItem
                )
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor
            ),
            onClick = {}
        ) {
            Text(
                "Share highlights"
            )
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
                                append(
                                    text = item,

                                    )
                            }
                        },
                        lineHeight = 21.sp
                    )
                }
            }
        }
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
            )
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
