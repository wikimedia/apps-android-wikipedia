package org.wikipedia.activitytab

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.TinyBarChart
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReadingHistoryModule(
    modifier: Modifier,
    userName: String,
    readingHistoryState: UiState<ActivityTabViewModel.ReadingHistory>,
    onArticlesReadClick: () -> Unit = {},
    onArticlesSavedClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onCategoryItemClick: (Category) -> Unit = {},
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    Text(
        text = stringResource(R.string.activity_tab_user_reading, userName),
        modifier = modifier
            .padding(top = 16.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        color = WikipediaTheme.colors.primaryColor
    )
    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(
                color = WikipediaTheme.colors.additionColor,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Text(
            text = stringResource(R.string.activity_tab_on_wikipedia_android).uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = TextUnit(0.8f, TextUnitType.Sp),
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )
    }
    if (readingHistoryState is UiState.Loading) {
        CircularProgressIndicator(
            modifier = modifier.padding(vertical = 16.dp).size(48.dp),
            color = WikipediaTheme.colors.progressiveColor
        )
    } else if (readingHistoryState is UiState.Success) {
        val readingHistory = readingHistoryState.data
        val todayDate = LocalDate.now()

        Text(
            text = stringResource(
                R.string.activity_tab_weekly_time_spent_hm,
                (readingHistory.timeSpentThisWeek / 3600),
                (readingHistory.timeSpentThisWeek % 60)
            ),
            modifier = modifier.padding(top = 12.dp),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineLarge.copy(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ComposeColors.Red700,
                        ComposeColors.Orange500,
                        ComposeColors.Yellow500,
                        ComposeColors.Blue300
                    )
                )
            ),
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            text = stringResource(R.string.activity_tab_weekly_time_spent),
            modifier = modifier
                .padding(top = 8.dp, bottom = 16.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.primaryColor
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clickable {
                    onArticlesReadClick()
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = WikipediaTheme.colors.paperColor
            ),
            border = BorderStroke(
                width = 1.dp,
                color = WikipediaTheme.colors.borderColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = modifier.fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = modifier.weight(1f)
                ) {
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(R.drawable.ic_newsstand_24),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.activity_tab_monthly_articles_read),
                            style = MaterialTheme.typography.labelMedium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                    if (readingHistory.lastArticleReadTime != null) {
                        Text(
                            text = if (todayDate == readingHistory.lastArticleReadTime.toLocalDate())
                                readingHistory.lastArticleReadTime
                                    .format(
                                        DateTimeFormatter.ofPattern(
                                            DateFormat.getBestDateTimePattern(
                                                Locale.getDefault(),
                                                "hhmm a"
                                            )
                                        )
                                    )
                            else
                                readingHistory.lastArticleReadTime
                                    .format(
                                        DateTimeFormatter.ofPattern(
                                            DateFormat.getBestDateTimePattern(
                                                Locale.getDefault(),
                                                "MMMM d"
                                            )
                                        )
                                    ),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    }
                }
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                    tint = WikipediaTheme.colors.secondaryColor,
                    contentDescription = null
                )
            }

            Row(
                modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)
            ) {
                Text(
                    text = readingHistory.articlesReadThisMonth.toString(),
                    modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.weight(1f))

                TinyBarChart(
                    values = readingHistory.articlesReadByWeek,
                    modifier = Modifier.padding(end = 16.dp).size(
                        72.dp,
                        if (readingHistory.articlesReadThisMonth == 0) 32.dp else 48.dp
                    ),
                    minColor = ComposeColors.Gray300,
                    maxColor = ComposeColors.Green600
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                .clickable {
                    onArticlesSavedClick()
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = WikipediaTheme.colors.paperColor
            ),
            border = BorderStroke(
                width = 1.dp,
                color = WikipediaTheme.colors.borderColor
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = modifier.fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Column(
                    modifier = modifier.weight(1f)
                ) {
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(16.dp),
                            painter = painterResource(R.drawable.ic_bookmark_border_white_24dp),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.activity_tab_monthly_articles_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                    if (readingHistory.lastArticleSavedTime != null) {
                        Text(
                            text = if (todayDate == readingHistory.lastArticleSavedTime.toLocalDate())
                                readingHistory.lastArticleSavedTime
                                    .format(
                                        DateTimeFormatter.ofPattern(
                                            DateFormat.getBestDateTimePattern(
                                                Locale.getDefault(),
                                                "hhmm a"
                                            )
                                        )
                                    )
                            else
                                readingHistory.lastArticleSavedTime
                                    .format(
                                        DateTimeFormatter.ofPattern(
                                            DateFormat.getBestDateTimePattern(
                                                Locale.getDefault(),
                                                "MMMM d"
                                            )
                                        )
                                    ),
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    }
                }
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                    tint = WikipediaTheme.colors.secondaryColor,
                    contentDescription = null
                )
            }
            Row(
                modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 16.dp)
            ) {
                Text(
                    text = readingHistory.articlesSavedThisMonth.toString(),
                    modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    val itemsToShow =
                        if (readingHistory.articlesSaved.size <= 4) readingHistory.articlesSaved.size else 3
                    val showOverflowItem = readingHistory.articlesSaved.size > 4

                    for (i in 0 until itemsToShow) {
                        val url = readingHistory.articlesSaved[i].thumbUrl
                        if (url == null) {
                            Box(
                                modifier = Modifier.padding(start = 4.dp).size(38.dp)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(19.dp)
                                    ).border(
                                        0.5.dp,
                                        WikipediaTheme.colors.borderColor,
                                        RoundedCornerShape(19.dp)
                                    )
                            ) {
                                Icon(
                                    modifier = Modifier.size(24.dp).align(Alignment.Center),
                                    painter = painterResource(R.drawable.ic_wikipedia_b),
                                    tint = WikipediaTheme.colors.primaryColor,
                                    contentDescription = null
                                )
                            }
                        } else {
                            val request =
                                ImageService.getRequest(LocalContext.current, url = url)
                            AsyncImage(
                                model = request,
                                placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                                error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                modifier = Modifier.padding(start = 4.dp).size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                            )
                        }
                    }

                    if (showOverflowItem) {
                        Box(
                            modifier = Modifier.padding(start = 4.dp).size(38.dp)
                                .background(
                                    color = WikipediaTheme.colors.placeholderColor,
                                    shape = RoundedCornerShape(19.dp)
                                )
                        ) {
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "+%d",
                                    readingHistory.articlesSavedThisMonth - 3
                                ),
                                modifier = Modifier.align(Alignment.Center),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (readingHistory.topCategories.isNotEmpty()) {
            TopCategoriesView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                categories = readingHistory.topCategories,
                onClick = {
                    onCategoryItemClick(it)
                }
            )
        }

        if (readingHistory.articlesReadThisMonth == 0 && readingHistory.articlesSavedThisMonth == 0) {
            Text(
                text = stringResource(R.string.activity_tab_discover_encourage),
                modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = WikipediaTheme.colors.primaryColor
            )
            Button(
                modifier = modifier.padding(top = 8.dp, bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WikipediaTheme.colors.progressiveColor,
                    contentColor = WikipediaTheme.colors.paperColor,
                ),
                onClick = {
                    onExploreClick()
                },
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_globe),
                    tint = WikipediaTheme.colors.paperColor,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = stringResource(R.string.activity_tab_explore_wikipedia)
                )
            }
        }
    } else if (readingHistoryState is UiState.Error) {
        Box(
            modifier = modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WikiErrorView(
                modifier = Modifier
                    .fillMaxWidth(),
                caught = readingHistoryState.error,
                errorClickEvents = wikiErrorClickEvents
            )
        }
    }
}
