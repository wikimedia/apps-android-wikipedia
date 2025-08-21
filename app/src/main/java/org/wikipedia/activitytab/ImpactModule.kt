package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.LineChart
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact.ArticleViews
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ImpactModule(
    modifier: Modifier = Modifier,
    uiState: UiState<GrowthUserImpact>,
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
                // TODO: list item click
            }
        )
    }
}

@Composable
fun MostViewedCard(
    modifier: Modifier = Modifier,
    data: Map<PageSummary, ArticleViews>,
    onClick: (() -> Unit)? = null
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
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
            Row(
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
                val iconResource = when (index++) {
                    1 -> R.drawable.outline_looks_one_24
                    2 -> R.drawable.outline_looks_two_24
                    else -> R.drawable.outline_looks_three_24
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(iconResource),
                        tint = WikipediaTheme.colors.primaryColor,
                        contentDescription = null
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Text(
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
                        ) {
                            LineChart(
                                map = articleViews.viewsByDay,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                strokeWidth = 2.dp,
                                strokeColor = WikipediaTheme.colors.progressiveColor
                            )
                            Text(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .align(Alignment.CenterVertically),
                                text = formatter.format(articleViews.viewsCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = WikipediaTheme.colors.progressiveColor
                            )
                        }
                    }
                    if (pageSummary.thumbnailUrl != null) {
                        val request =
                            ImageService.getRequest(LocalContext.current, url = pageSummary.thumbnailUrl)
                        AsyncImage(
                            model = request,
                            placeholder = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                            error = BrushPainter(SolidColor(WikipediaTheme.colors.borderColor)),
                            contentScale = ContentScale.Crop,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContributionCard(
    modifier: Modifier = Modifier,
    groupEditsByMonth: Map<String, Int>,
    onClick: (() -> Unit)? = null
) {
    // TODO: Implement the UI for the ContributionCard
}

@Composable
fun AllTimeImpactCard(
    modifier: Modifier = Modifier,
    impact: GrowthUserImpact,
    onClick: (() -> Unit)? = null
) {
    // TODO: Implement the UI for the EditsStatsCard
}

@Composable
fun EditsStatView(
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
            )
        )
    }
}
