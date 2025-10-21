package org.wikipedia.activitytab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.LineChart
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.theme.Theme
import org.wikipedia.util.DateUtil
import org.wikipedia.util.UiState
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImpactModule(
    modifier: Modifier = Modifier,
    uiState: UiState<GrowthUserImpact>,
    wikiErrorClickEvents: WikiErrorClickEvents? = null
) {
    when (uiState) {
        UiState.Loading -> {
            ActivityTabShimmerView(size = 340.dp)
        }
        is UiState.Success -> {
            AllTimeImpactCard(
                modifier = modifier
                    .fillMaxWidth(),
                totalEdits = uiState.data.totalEditsCount,
                totalThanks = uiState.data.receivedThanksCount,
                longestEditingStreak = uiState.data.longestEditingStreak?.totalEditCountForPeriod ?: 0,
                lastEditTimestamp = uiState.data.lastEditTimestamp,
                lastThirtyDaysEdits = uiState.data.lastThirtyDaysEdits,
                totalPageviewsCount = uiState.data.totalPageviewsCount,
                dailyTotalViews = uiState.data.dailyTotalViews
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
                    errorClickEvents = wikiErrorClickEvents,
                    retryForGenericError = true
                )
            }
        }
    }
}

@Composable
fun AllTimeImpactCard(
    modifier: Modifier = Modifier,
    totalEdits: Int = 0,
    totalThanks: Int = 0,
    longestEditingStreak: Int = 0,
    lastEditTimestamp: Long = 0,
    lastThirtyDaysEdits: Map<String, Int> = emptyMap(),
    totalPageviewsCount: Long = 0,
    dailyTotalViews: Map<String, Int> = emptyMap()
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
                val bestStreakString = if (longestEditingStreak > 0) {
                    pluralStringResource(R.plurals.activity_tab_impact_best_streak_text, longestEditingStreak, longestEditingStreak)
                } else {
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
                    DateUtil.getMDYDateString(Date(lastEditTimestamp * 1000))
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

            if (totalEdits > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 16.dp),
                    color = WikipediaTheme.colors.borderColor
                )

                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.activity_tab_impact_recent_activity),
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val recentEditsCount = lastThirtyDaysEdits.values.sum()
                        Text(
                            text = recentEditsCount.toString(),
                            modifier = Modifier.align(Alignment.Bottom),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.activity_tab_impact_recent_activity_edits,
                                recentEditsCount
                            ).lowercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            ),
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        val totalSize = lastThirtyDaysEdits.size
                        lastThirtyDaysEdits.forEach { (_, editCount) ->
                            val pileColor = if (editCount > 0) {
                                WikipediaTheme.colors.progressiveColor
                            } else {
                                WikipediaTheme.colors.borderColor
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f / totalSize)
                                    .height(24.dp)
                                    .padding(horizontal = 1.dp)
                                    .background(color = pileColor, shape = RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = DateUtil.getMonthOnlyDateStringFromTimeString(lastThirtyDaysEdits.keys.first()),
                            style = MaterialTheme.typography.bodySmall,
                            color = WikipediaTheme.colors.secondaryColor
                        )
                        Text(
                            text = DateUtil.getMonthOnlyDateStringFromTimeString(lastThirtyDaysEdits.keys.last()),
                            style = MaterialTheme.typography.bodySmall,
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(top = 16.dp),
                        color = WikipediaTheme.colors.borderColor
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = formatter.format(totalPageviewsCount),
                            modifier = Modifier.align(Alignment.Bottom),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = WikipediaTheme.colors.primaryColor
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.activity_tab_impact_recent_activity_views,
                                totalPageviewsCount.toInt()
                            ).lowercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 22.sp
                            ),
                            color = WikipediaTheme.colors.secondaryColor
                        )
                    }

                    LineChart(
                        map = dailyTotalViews,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(top = 8.dp),
                        chartSampleSize = 90,
                        strokeWidth = 2.dp,
                        strokeColor = WikipediaTheme.colors.progressiveColor
                    )
                }
            }
        }
    }
}

@Composable
fun TotalEditsCard(
    modifier: Modifier = Modifier,
    uiState: UiState<Int>,
    onClickListener: () -> Unit = {}
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    when (uiState) {
        UiState.Loading -> { ActivityTabShimmerView() }
        is UiState.Success -> {
            WikiCard(
                modifier = modifier,
                elevation = 0.dp,
                border = BorderStroke(width = 1.dp, color = WikipediaTheme.colors.borderColor),
                onClick = onClickListener
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(R.drawable.ic_public_24),
                                tint = WikipediaTheme.colors.primaryColor,
                                contentDescription = null
                            )
                            Text(
                                text = stringResource(R.string.activity_tab_total_edits_all_projects),
                                style = MaterialTheme.typography.labelMedium,
                                color = WikipediaTheme.colors.primaryColor,
                                lineHeight = MaterialTheme.typography.labelMedium.lineHeight
                            )
                        }
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(R.drawable.ic_chevron_forward_white_24dp),
                            tint = WikipediaTheme.colors.secondaryColor,
                            contentDescription = null
                        )
                    }
                    Text(
                        modifier = Modifier.padding(top = 16.dp),
                        text = formatter.format(uiState.data),
                        style = MaterialTheme.typography.titleLarge,
                        color = WikipediaTheme.colors.primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        is UiState.Error -> {}
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
private fun AllTimeImpactCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        AllTimeImpactCard(
            modifier = Modifier.fillMaxWidth(),
            totalEdits = 123,
            totalThanks = 56,
            longestEditingStreak = 15,
            lastEditTimestamp = System.currentTimeMillis() - 86400000L,
            lastThirtyDaysEdits = mapOf(
                "2023-10-01" to 5,
                "2023-10-02" to 3,
                "2023-10-03" to 7,
                "2023-10-04" to 0,
                "2023-10-05" to 4,
                "2023-10-06" to 6,
                "2023-10-07" to 2,
                "2023-10-08" to 1,
                "2023-10-09" to 0,
                "2023-10-10" to 8,
                "2023-10-11" to 0,
                "2023-10-12" to 3,
                "2023-10-13" to 5,
                "2023-10-14" to 2,
                "2023-10-15" to 4,
                "2023-10-16" to 0,
                "2023-10-17" to 1,
                "2023-10-18" to 0,
                "2023-10-19" to 6,
                "2023-10-20" to 2,
                "2023-10-21" to 3,
                "2023-10-22" to 0,
                "2023-10-23" to 4,
                "2023-10-24" to 5,
                "2023-10-25" to 0,
                "2023-10-26" to 1,
                "2023-10-27" to 2,
                "2023-10-28" to 0,
                "2023-10-29" to 3,
                "2023-10-30" to 4,
                "2023-10-31" to 0
            ),
            totalPageviewsCount = 7890,
            dailyTotalViews = mapOf(
                "2023-10-01" to 100,
                "2023-10-02" to 150,
                "2023-10-03" to 200,
                "2023-10-04" to 120,
                "2023-10-05" to 180,
                "2023-10-06" to 220
            )
        )
    }
}

@Preview
@Composable
private fun TotalEditsCardPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        TotalEditsCard(
            modifier = Modifier.fillMaxWidth(),
            uiState = UiState.Success(1234)
        )
    }
}
