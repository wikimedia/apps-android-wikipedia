package org.wikipedia.feed

import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.HtmlText
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.getString
import org.wikipedia.theme.Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CommunityDisclaimer(
    modifier: Modifier,
    wikiSite: WikiSite
) {
    Box(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = WikipediaTheme.colors.borderColor,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_disclaimer),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
            Image(
                modifier = Modifier.size(45.dp),
                painter = painterResource(R.drawable.w_nav_mark),
                contentDescription = null
            )
        }
    }
}
@Composable
fun DayHeader(date: LocalDate, isFirst: Boolean = true) {
    val dateFormatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(LocalLocale.current.platformLocale, "MMM dd, yyyy"))
    Text(
        text = if (LocalDate.now().dayOfYear == date.dayOfYear) stringResource(R.string.explore_feed_date_today, date.format(dateFormatter)) else date.format(dateFormatter),
        color = WikipediaTheme.colors.secondaryColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = if (isFirst) 16.dp else 24.dp)
    )
}

@Composable
fun LoadMoreButton(
    wikiSite: WikiSite,
    isCommunity: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isCommunity) {
            AppButton(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                onClick = onClick,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dynamic_feed_24dp),
                        tint = WikipediaTheme.colors.paperColor,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = LocalContext.current.getString(wikiSite.languageCode, R.string.explore_feed_community_load_more_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = WikipediaTheme.colors.paperColor
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onClick) {
                    Text(
                        text = "Load more recommendations",
                        color = WikipediaTheme.colors.progressiveColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = WikipediaTheme.colors.progressiveColor
        )
    }
}

@Composable
fun ErrorState(caught: Throwable, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WikiErrorView(
            modifier = Modifier,
            caught,
            errorClickEvents = WikiErrorClickEvents(
                retryClickListener = {
                    onRetry()
                }
            ),
            retryForGenericError = true
        )
    }
}

@Composable
fun FeedEmptyStateView(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    buttonText: String,
    onCallToActionClick: () -> Unit,
) {
    Column (
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier
                .padding(bottom = 16.dp),
            painter = painterResource(R.drawable.empty_feed_illustration),
            contentDescription = null
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.15.sp
            ),
            color = WikipediaTheme.colors.secondaryColor
        )
        HtmlText(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                letterSpacing = 0.25.sp
            ),
            textAlign = TextAlign.Center,
            color = WikipediaTheme.colors.secondaryColor
        )
        AppButton(
            onClick = onCallToActionClick
        ) {
            Text(
                text = buttonText
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun CommunityDisclaimerPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        CommunityDisclaimer(
            modifier = Modifier
                .padding(16.dp)
                .height(72.dp),
            wikiSite = WikiSite.preview()
        )
    }
}

@Preview
@Composable
fun LoadMoreButtonPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        LoadMoreButton(
            wikiSite = WikiSite.preview(),
            isCommunity = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
fun DayHeaderPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        DayHeader(LocalDate.now())
    }
}
