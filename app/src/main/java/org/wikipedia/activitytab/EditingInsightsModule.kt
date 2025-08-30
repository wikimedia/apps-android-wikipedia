package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.wikipedia.R
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.LineChart
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact.ArticleViews
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import org.wikipedia.views.imageservice.ImageService
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EditingInsightsModule(
    modifier: Modifier = Modifier,
    uiState: UiState<GrowthUserImpact>,
    onPageItemClick: (PageTitle) -> Unit,
    onContributionClick: (() -> Unit),
    onSuggestedEditsClick: (() -> Unit),
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    when (uiState) {
        UiState.Loading -> {
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
        }
        is UiState.Success -> {
            MostViewedCard(
                modifier = modifier
                    .fillMaxWidth(),
                data = uiState.data.topViewedArticlesWithPageTitle,
                onClick = {
                    onPageItemClick(it)
                }
            )
            ContributionCard(
                modifier = modifier
                    .fillMaxWidth(),
                lastEditRelativeTime = uiState.data.lastEditRelativeTime,
                editsThisMonth = uiState.data.editsThisMonth,
                editsLastMonth = uiState.data.editsLastMonth,
                totalEdits = uiState.data.totalEditsCount,
                onContributionClick = {
                    onContributionClick()
                },
                onSuggestedEditsClick = {
                    onSuggestedEditsClick()
                }
            )
        }

        is UiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                WikiErrorView(
                    modifier = Modifier
                        .fillMaxWidth(),
                    caught = uiState.error,
                    errorClickEvents = wikiErrorClickEvents
                )
            }
        }
    }
}

@Composable
fun MostViewedCard(
    modifier: Modifier = Modifier,
    data: Map<PageTitle, ArticleViews>,
    showSize: Int = 3,
    onClick: (PageTitle) -> Unit
) {
    if (data.isEmpty()) {
        return
    }
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
            data.forEach { (pageTitle, articleViews) ->
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
                        .clickable(onClick = { onClick(pageTitle) })
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
                                text = pageTitle.displayText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif
                                ),
                                color = WikipediaTheme.colors.primaryColor,
                            )
                            pageTitle.description?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WikipediaTheme.colors.secondaryColor
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
                        if (pageTitle.thumbUrl != null) {
                            val request =
                                ImageService.getRequest(
                                    LocalContext.current,
                                    url = pageTitle.thumbUrl
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
    totalEdits: Int = 0,
    onContributionClick: (() -> Unit)? = null,
    onSuggestedEditsClick: (() -> Unit)? = null
) {
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
        ),
        onClick = onContributionClick
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

                ContributionEditsView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = pluralStringResource(
                        R.plurals.activity_tab_impact_edits_this_month,
                        editsThisMonth
                    ).lowercase(),
                    edits = editsThisMonth,
                    maxEdits = maxEditsCount,
                    barColor = WikipediaTheme.colors.successColor
                )

                ContributionEditsView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = pluralStringResource(
                        R.plurals.activity_tab_impact_edits_last_month,
                        editsLastMonth
                    ).lowercase(),
                    edits = editsLastMonth,
                    maxEdits = maxEditsCount,
                    barColor = WikipediaTheme.colors.borderColor
                )
            }
            if (totalEdits == 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    color = WikipediaTheme.colors.borderColor
                )
                SuggestedEditsView(
                    onClick = onSuggestedEditsClick
                )
            }
        }
    }
}

@Composable
fun SuggestedEditsView(
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(R.string.activity_tab_impact_suggested_edits_title),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(R.string.activity_tab_impact_suggested_edits_message),
            style = MaterialTheme.typography.bodyMedium,
            color = WikipediaTheme.colors.primaryColor
        )
        Button(
            modifier = Modifier.padding(top = 16.dp).align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WikipediaTheme.colors.progressiveColor,
                contentColor = WikipediaTheme.colors.paperColor,
            ),
            onClick = { onClick?.invoke() },
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.ic_mode_edit_white_24dp),
                tint = WikipediaTheme.colors.paperColor,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 4.dp),
                text = stringResource(R.string.activity_tab_impact_suggested_edits_button),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun ContributionEditsView(
    modifier: Modifier,
    text: String,
    edits: Int,
    maxEdits: Float,
    barColor: Color
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = edits.toString(),
            modifier = Modifier.padding(start = 16.dp).align(Alignment.Bottom),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = WikipediaTheme.colors.primaryColor
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            ),
            color = WikipediaTheme.colors.secondaryColor
        )
    }

    if (edits > 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth(edits.toFloat() / maxEdits)
                .padding(horizontal = 16.dp)
                .height(20.dp)
                .background(
                    color = barColor,
                    shape = RoundedCornerShape(16.dp)
                )
        )
    }
}

@Preview
@Composable
private fun MostViewedCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        val pageTitle = PageTitle(
            text = "Test Article",
            displayText = "Test Article",
            wiki = WikiSite("en.wikipedia.org".toUri(), "en"),
            description = "This is a test article",
            thumbUrl = "https://example.com/thumb.jpg"
        )
        val articleViews = ArticleViews(
            firstEditDate = "2023-01-01",
            newestEdit = "2023-10-01",
            imageUrl = "https://example.com/image.jpg",
            viewsCount = 1000
        )
        MostViewedCard(
            data = mapOf(
                pageTitle to articleViews
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
            totalEdits = 9,
            editsThisMonth = 9,
            editsLastMonth = 2,
            onContributionClick = null,
            onSuggestedEditsClick = null
        )
    }
}
