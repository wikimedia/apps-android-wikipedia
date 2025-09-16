package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.UriUtil

@Composable
fun YearInReviewOnboardingScreen(
    modifier: Modifier = Modifier,
    contentData: YearInReviewScreenData,
    onNavigationBackButtonClick: () -> Unit,
    onGetStartedClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            YearInReviewTopBar(
                onNavigationBackButtonClick = onNavigationBackButtonClick
            )
        },
        bottomBar = {
            OnboardingBottomBar(onGetStartedClick = onGetStartedClick)
        },
        content = { paddingValues ->
            YearInReviewScreenContent(
                innerPadding = paddingValues,
                screenData = contentData,
                screenCaptureMode = false,
                isOnboardingScreen = true,
            )
        }
    )
}

@Composable
fun OnboardingBottomBar(
    onGetStartedClick: () -> Unit
) {
    val context = LocalContext.current
    BottomAppBar(
        containerColor = WikipediaTheme.colors.paperColor,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp)
            ) {
                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.paperColor,
                        contentColor = WikipediaTheme.colors.progressiveColor),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = {
                        UriUtil.handleExternalLink(
                            context = context,
                            uri = context.getString(R.string.year_in_review_media_wiki_url).toUri()
                        )
                    }
                ) {
                    Text(
                        text = stringResource(R.string.year_in_review_learn_more),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor,
                        contentColor = WikipediaTheme.colors.paperColor
                    ),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = { onGetStartedClick() }
                ) {
                    Text(
                        text = stringResource(R.string.year_in_review_get_started),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    )
}
