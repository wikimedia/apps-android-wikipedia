package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.LineChart
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact.ArticleViews
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.theme.Theme
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImpactModule(
    modifier: Modifier = Modifier,
    uiState: UiState<GrowthUserImpact>,
    onPageItemClick: (PageSummary) -> Unit,
    onContributionClick: (() -> Unit),
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    if (uiState == UiState.Loading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                color = WikipediaTheme.colors.progressiveColor
            )
        }
    } else if (uiState is UiState.Success) {
        MostViewedCard(
            modifier = modifier
                .fillMaxWidth(),
            data = uiState.data.topViewedArticlesWithPageSummary,
            onClick = {
                onPageItemClick(it)
            }
        )
        ContributionCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            lastEditRelativeTime = uiState.data.lastEditRelativeTime,
            editsThisMonth = uiState.data.editsThisMonth,
            editsLastMonth = uiState.data.editsLastMonth,
            onClick = {
                onContributionClick()
            }
        )
    }
}

@Composable
fun MostViewedCard(
    modifier: Modifier = Modifier,
    data: Map<PageSummary, ArticleViews>,
    showSize: Int = 3,
    onClick: (PageSummary) -> Unit
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    WikiCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.outline_trending_up_24),
                    tint = WikipediaTheme.colors.primaryColor,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.activity_tab_impact_most_viewed),
                    style = MaterialTheme.typography.labelMedium,
                    color = WikipediaTheme.colors.primaryColor
                )
            }

            var index = 1
            data.forEach { (pageSummary, articleViews) ->
                if (index > showSize) return@forEach
                var iconResource = when (index++) {
                    1 -> R.drawable.outline_looks_one_24
                    2 -> R.drawable.outline_looks_two_24
                    3 -> R.drawable.outline_looks_three_24
                    else -> null
                }
                if (data.size <= 1) {
                    iconResource = null
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { onClick(pageSummary) })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        iconResource?.let {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(it),
                                tint = WikipediaTheme.colors.primaryColor,
                                contentDescription = null
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            HtmlText(
                                text = pageSummary.displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif
                                ),
                                color = WikipediaTheme.colors.primaryColor,
                            )
                            pageSummary.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WikipediaTheme.colors.secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                LineChart(
                                    map = articleViews.viewsByDay,
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(6.dp),
                                    chartSampleSize = 10,
                                    strokeWidth = 1.dp,
                                    strokeColor = WikipediaTheme.colors.progressiveColor
                                )
                                Text(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                    text = formatter.format(articleViews.viewsCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WikipediaTheme.colors.progressiveColor
                                )
                            }
                        }
                        if (pageSummary.thumbnailUrl != null) {
                            val request =
                                ImageService.getRequest(
                                    LocalContext.current,
                                    url = pageSummary.thumbnailUrl
                                )
                            AsyncImage(
                                model = request,
                                placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                                error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
                if (index < data.size - 1) {
                    HorizontalDivider(
                        color = WikipediaTheme.colors.borderColor
                    )
                }
            }
        }
    }
}

@Composable
fun ContributionCard(
    modifier: Modifier = Modifier,
    lastEditRelativeTime: String,
    editsThisMonth: Int,
    editsLastMonth: Int,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
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
                            painter = painterResource(R.drawable.ic_icon_user_contributions_ooui),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                        Text(
                            text = stringResource(R.string.activity_tab_impact_contributions_this_month),
                            style = MaterialTheme.typography.labelMedium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }
                    Text(
                        text = lastEditRelativeTime,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                    tint = WikipediaTheme.colors.secondaryColor,
                    contentDescription = null
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                val maxEditsCount = maxOf(editsThisMonth, editsLastMonth).toFloat()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = editsThisMonth.toString(),
                        modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.activity_tab_impact_edits_this_month,
                            editsThisMonth
                        ).lowercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        ),
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }

                if (editsThisMonth > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(editsThisMonth.toFloat() / maxEditsCount)
                            .padding(horizontal = 16.dp)
                            .height(20.dp)
                            .background(
                                color = WikipediaTheme.colors.successColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = editsLastMonth.toString(),
                        modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.primaryColor
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.activity_tab_impact_edits_last_month,
                            editsLastMonth
                        ).lowercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        ),
                        color = WikipediaTheme.colors.secondaryColor
                    )
                }

                if (editsLastMonth > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(editsLastMonth.toFloat() / maxEditsCount)
                            .padding(horizontal = 16.dp)
                            .height(20.dp)
                            .background(
                                color = WikipediaTheme.colors.borderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun AllTimeImpactCard(
    modifier: Modifier = Modifier,
    totalEdits: Int = 0,
    totalThanks: Int = 0,
    longestEditingStreak: Int? = null,
    lastEditTimestamp: Long = 0,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        colors = CardDefaults.cardColors(
            containerColor = WikipediaTheme.colors.paperColor,
            contentColor = WikipediaTheme.colors.paperColor
        ),
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.activity_tab_impact_all_time),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = WikipediaTheme.colors.primaryColor,
                lineHeight = MaterialTheme.typography.labelMedium.lineHeight
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ImpactStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.ic_mode_edit_white_24dp,
                    statValue = totalEdits.toString(),
                    statLabel = pluralStringResource(R.plurals.activity_tab_impact_total_edits, totalEdits)
                )
                val bestStreakString = longestEditingStreak?.let {
                    pluralStringResource(R.plurals.activity_tab_impact_best_streak_text, it, it)
                } ?: run {
                    "-"
                }
                ImpactStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.baseline_stars_24,
                    statValue = bestStreakString,
                    statLabel = stringResource(R.string.activity_tab_impact_best_streak)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ImpactStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.ic_notification_thanks,
                    statValue = totalThanks.toString(),
                    statLabel = pluralStringResource(R.plurals.activity_tab_impact_thanks, totalThanks)
                )
                val lastEditedDateString = if (lastEditTimestamp > 0) {
                    DateUtil.getMDYDateString(Date(lastEditTimestamp))
                } else {
                    "-"
                }
                ImpactStatView(
                    modifier = Modifier.weight(1f),
                    iconResource = R.drawable.edit_history_ooui,
                    statValue = lastEditedDateString,
                    statLabel = stringResource(R.string.activity_tab_impact_last_edited)
                )
            }
        }
    }
}

@Composable
fun ImpactStatView(
    modifier: Modifier,
    iconResource: Int,
    statValue: String,
    statLabel: String
) {
    Row(
        modifier = modifier
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = painterResource(iconResource),
            tint = WikipediaTheme.colors.progressiveColor,
            contentDescription = null
        )
        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = statValue,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = statLabel.lowercase(),
                style = MaterialTheme.typography.bodySmall,
                color = WikipediaTheme.colors.primaryColor
            )
        }
    }
}

@Preview
@Composable
private fun MostViewedCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        val pageSummary = PageSummary(
            displayTitle = "Wikipedia",
            prefixTitle = "Wikipedia",
            description = "The free encyclopedia",
            extract = null,
            thumbnail = null,
            lang = "en"
        )
        val articleViews = ArticleViews(
            firstEditDate = "2023-01-01",
            newestEdit = "2023-10-01",
            imageUrl = "https://example.com/image.jpg",
            viewsCount = 1000
        )
        MostViewedCard(
            data = mapOf(
                pageSummary to articleViews
            ),
            onClick = { },
        )
    }
}

@Preview
@Composable
private fun ContributionCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        ContributionCard(
            modifier = Modifier.fillMaxWidth(),
            lastEditRelativeTime = "2 days ago",
            editsThisMonth = 9,
            editsLastMonth = 2,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun AllTimeImpactCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        AllTimeImpactCard(
            modifier = Modifier.fillMaxWidth(),
            totalEdits = 1234,
            totalThanks = 56,
            longestEditingStreak = 15,
            lastEditTimestamp = System.currentTimeMillis() - 86400000L,
            onClick = {}
        )
    }
}
