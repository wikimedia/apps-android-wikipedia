package org.wikipedia.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PlacesEvent
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.language.LanguageUtil
import org.wikipedia.theme.Theme

@Composable
fun NoSearchResults(
    countsPerLanguageCode: List<Pair<String, Int>>,
    onLanguageClick: (Int) -> Unit,
    invokeSource: Constants.InvokeSource,
    modifier: Modifier = Modifier
) {
    if (countsPerLanguageCode.isNotEmpty() && invokeSource == Constants.InvokeSource.PLACES) {
        LaunchedEffect(Unit) {
            PlacesEvent.logAction("no_results_impression", "search_view")
        }
    }

    if (countsPerLanguageCode.isNotEmpty()) {
        LazyColumn(
            modifier = modifier
        ) {
            items(countsPerLanguageCode.size) { index ->
                val (langCode, count) = countsPerLanguageCode[index]
                val color =
                    if (count == 0) WikipediaTheme.colors.secondaryColor else WikipediaTheme.colors.progressiveColor

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (count > 0) {
                                Modifier.clickable(onClick = { onLanguageClick(index) })
                            } else Modifier
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (count == 0) stringResource(R.string.search_results_count_zero) else pluralStringResource(
                            R.plurals.search_results_count,
                            count,
                            count
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = WikipediaTheme.colors.secondaryColor,
                        modifier = Modifier
                            .weight(1f)
                    )

                    if (countsPerLanguageCode.size > 1) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .background(color = WikipediaTheme.colors.paperColor).border(
                                    1.5.dp,
                                    color,
                                    RoundedCornerShape(4.dp)
                                )
                                .size(20.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BasicText(
                                modifier = Modifier.padding(0.5.dp),
                                text = LanguageUtil.formatLangCodeForButton(langCode.uppercase()),
                                autoSize = TextAutoSize.StepBased(minFontSize = 1.sp, maxFontSize = 10.sp, stepSize = 1.sp),
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    textAlign = TextAlign.Center
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun NoSearchResultsPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        NoSearchResults(
            modifier = Modifier
                .padding(16.dp),
            countsPerLanguageCode = listOf(
                "en" to 10,
                "ne" to 5,
                "ZH-TW" to 7,
            ),
            onLanguageClick = {},
            invokeSource = Constants.InvokeSource.SEARCH,
        )
    }
}
