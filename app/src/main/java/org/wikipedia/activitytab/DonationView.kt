package org.wikipedia.activitytab

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.WikiCard
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import java.time.LocalDateTime
import java.time.ZoneOffset

@Composable
fun DonationView(
    modifier: Modifier = Modifier,
    uiState: UiState<String?>,
    wikiErrorClickEvents: WikiErrorClickEvents? = null,
    onClick: (() -> Unit)? = null
) {
    WikiCard(
        modifier = modifier
            .clickable(onClick = { onClick?.invoke() }),
        elevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = WikipediaTheme.colors.borderColor
        )
    ) {
        when (uiState) {
            is UiState.Error -> {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(200.dp),
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
                val timestamp = uiState.data
                // use the relative time from the last donation
                val lastDonationTime = timestamp?.let {
                    val timestampInLong = LocalDateTime.parse(timestamp).toInstant(ZoneOffset.UTC).epochSecond
                    val relativeTime = DateUtils.getRelativeTimeSpanString(
                        timestampInLong * 1000, // Convert seconds to milliseconds
                        System.currentTimeMillis(),
                        0L
                    )
                    return@let relativeTime.toString()
                } ?: "Unknown"
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row {
                        Icon(
                            modifier = Modifier
                                .size(16.dp),
                            painter = painterResource(R.drawable.outline_credit_card_heart_24),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 16.dp),
                            text = "Last donation",
                            style = MaterialTheme.typography.labelMedium,
                            color = WikipediaTheme.colors.primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            modifier = Modifier.padding(end = 16.dp),
                            text = " in app",
                            style = MaterialTheme.typography.bodySmall,
                            color = WikipediaTheme.colors.primaryColor
                        )
                    }

                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        text = lastDonationTime,
                        style = MaterialTheme.typography.titleLarge,
                        color = WikipediaTheme.colors.progressiveColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DonationViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        DonationView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            uiState = UiState.Success("2023-10-01T12:00:00Z"),
            onClick = {}
        )
    }
}
