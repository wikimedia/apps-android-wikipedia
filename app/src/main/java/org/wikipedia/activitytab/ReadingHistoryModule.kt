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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.categories.db.Category
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.TinyBarChart
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ReadingHistoryModule(
    modifier: Modifier,
    userName: String,
    showTimeSpent: Boolean,
    showInsights: Boolean,
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

        if (showTimeSpent) {
            Text(
                text = stringResource(
                    R.string.activity_tab_weekly_time_spent_hm,
                    (readingHistory.timeSpentThisWeek / 3600),
                    ((readingHistory.timeSpentThisWeek / 60) % 60)
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
                    .padding(top = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = WikipediaTheme.colors.primaryColor
            )
        }

        if (!showInsights) {
            Spacer(modifier = Modifier.height(16.dp))
            return
        }

        ArticleReadThisMonthCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            readingHistory = readingHistory,
            todayDate = todayDate,
            onClick = {
                onArticlesReadClick()
            }
        )

        ArticleSavedThisMonthCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            readingHistory = readingHistory,
            todayDate = todayDate,
            onClick = {
                onArticlesSavedClick()
            }
        )

        if (readingHistory.topCategories.isNotEmpty()) {
            TopCategoriesCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                categories = readingHistory.topCategories,
                onClick = {
                    onCategoryItemClick(it)
                }
            )
        }

        if (readingHistory.articlesReadThisMonth == 0) {
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
                    contentColor = Color.White,
                ),
                onClick = {
                    onExploreClick()
                },
            ) {
                Text(
                    text = stringResource(R.string.activity_tab_explore_wikipedia),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    } else if (readingHistoryState is UiState.Error) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            WikiErrorView(
                modifier = Modifier
                    .fillMaxWidth(),
                caught = readingHistoryState.error,
                errorClickEvents = wikiErrorClickEvents,
                retryForGenericError = true
            )
        }
    }
}

@Composable
private fun ArticleReadThisMonthCard(
    modifier: Modifier,
    readingHistory: ActivityTabViewModel.ReadingHistory,
    todayDate: LocalDate,
    onClick: () -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                Text(
                    text = readingHistory.articlesReadThisMonth.toString(),
                    modifier = Modifier.align(Alignment.Bottom),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.weight(1f))

                TinyBarChart(
                    values = readingHistory.articlesReadByWeek,
                    modifier = Modifier.size(
                        72.dp,
                        if (readingHistory.articlesReadThisMonth == 0) 32.dp else 48.dp
                    ),
                    minColor = ComposeColors.Gray300,
                    maxColor = ComposeColors.Green600
                )
            }
        }
    }
}

@Composable
private fun ArticleSavedThisMonthCard(
    modifier: Modifier,
    readingHistory: ActivityTabViewModel.ReadingHistory,
    todayDate: LocalDate,
    onClick: () -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                Text(
                    text = readingHistory.articlesSavedThisMonth.toString(),
                    modifier = Modifier.align(Alignment.Bottom),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.primaryColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    val itemsToShow =
                        if (readingHistory.articlesSaved.size <= 4) readingHistory.articlesSaved.size else 3
                    val showOverflowItem = readingHistory.articlesSaved.size > 4

                    for (i in 0 until itemsToShow) {
                        val url = readingHistory.articlesSaved[i].thumbUrl
                        if (url.isNullOrEmpty()) {
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
                                    tint = Color.Black,
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
    }
}

@Composable
fun TopCategoriesCard(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    onClick: (Category) -> Unit
) {
    WikiCard(
        modifier = modifier,
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_category_black_24dp),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.activity_tab_monthly_top_categories),
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
            }

            categories.forEachIndexed { index, value ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onClick(value) })
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                        text = StringUtil.removeNamespace(value.title),
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor
                    )
                }

                if (index < categories.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = WikipediaTheme.colors.borderColor
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ArticleReadThisMonthCardPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        ArticleReadThisMonthCard(
            modifier = Modifier
                .padding(20.dp),
            readingHistory = ActivityTabViewModel.ReadingHistory(
                articlesReadThisMonth = 42,
                articlesSavedThisMonth = 8,
                timeSpentThisWeek = 3661,
                lastArticleReadTime = LocalDate.now().atStartOfDay(),
                lastArticleSavedTime = LocalDate.now().atStartOfDay(),
                articlesReadByWeek = listOf(5, 10, 8, 12),
                articlesSaved = listOf(),
                topCategories = listOf()
            ),
            todayDate = LocalDate.now(),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun ArticleSavedThisMonthCardPreview() {
    val wikiSite = WikiSite("en.wikipedia.org".toUri(), "en")
    BaseTheme(currentTheme = Theme.LIGHT) {
        ArticleSavedThisMonthCard(
            modifier = Modifier
                .padding(20.dp),
            readingHistory = ActivityTabViewModel.ReadingHistory(
                articlesReadThisMonth = 42,
                articlesSavedThisMonth = 8,
                timeSpentThisWeek = 3661,
                lastArticleReadTime = LocalDate.now().atStartOfDay(),
                lastArticleSavedTime = LocalDate.now().atStartOfDay(),
                articlesReadByWeek = listOf(5, 10, 8, 12),
                articlesSaved = listOf(
                    PageTitle("Title1", wikiSite),
                    PageTitle("Title2", wikiSite),
                    PageTitle("Title3", wikiSite),
                ),
                topCategories = listOf()
            ),
            todayDate = LocalDate.now(),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun TopCategoriesViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        TopCategoriesCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            categories = listOf(
                Category(2025, 1, "Category:Ancient history", "en", 1),
                Category(2025, 1, "Category:World literature", "en", 1),
                Category(2025, 1, "Category:Cat breeds originating in the United States", "en", 1),
            ),
            onClick = {}
        )
    }
}
