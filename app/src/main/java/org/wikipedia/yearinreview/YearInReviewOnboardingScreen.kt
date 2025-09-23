package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.allowHardware
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.util.UriUtil

@Composable
fun YearInReviewOnboardingScreen(
    modifier: Modifier = Modifier,
    onBackButtonClick: () -> Unit,
    onGetStartedClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            YearInReviewTopBar(
                onNavigationBackButtonClick = onBackButtonClick
            )
        },
        bottomBar = {
            YearInReviewOnboardingBottomBar(onGetStartedClick = onGetStartedClick)
        },
        content = { paddingValues ->
            YearInReviewOnboardingContent(
                modifier = modifier.padding(paddingValues)
            )
        }
    )
}

@Composable
fun YearInReviewOnboardingContent(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val gifAspectRatio = 3f / 2f
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .verticalScroll(scrollState)
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(R.drawable.year_in_review_block_10_resize)
                .allowHardware(false)
                .build(),
            loading = { LoadingIndicator() },
            success = { SubcomposeAsyncImageContent() },
            contentDescription = stringResource(R.string.year_in_review_screendeck_image_content_description),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(gifAspectRatio)
                .clip(RoundedCornerShape(16.dp))
        )
        Column {
            Text(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 8.dp)
                    .height(IntrinsicSize.Min),
                text = stringResource(R.string.year_in_review_get_started_headline),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(IntrinsicSize.Min)
                    .weight(1f),
                text = stringResource(R.string.year_in_review_get_started_bodytext),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .height(IntrinsicSize.Min)
                    .weight(1f),
                text = stringResource(R.string.year_in_review_get_started_bodytext),
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun YearInReviewOnboardingBottomBar(
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

@Preview
@Composable
fun YearInReviewOnboardingScreenPreview() {
    BaseTheme {
        YearInReviewOnboardingScreen(
            onBackButtonClick = {},
            onGetStartedClick = {}
        )
    }
}
