package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import org.wikipedia.theme.Theme
import org.wikipedia.util.UriUtil

@OptIn(ExperimentalMaterial3Api::class)
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
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WikipediaTheme.colors.paperColor),
                title = {},
                navigationIcon = {
                    IconButton(onClick = { onBackButtonClick() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_close_black_24dp),
                            tint = WikipediaTheme.colors.primaryColor,
                            contentDescription = stringResource(R.string.year_in_review_close)
                        )
                    }
                }
            )
        },
        bottomBar = {
            YearInReviewOnboardingBottomBar(onGetStartedClick = onGetStartedClick)
        },
        content = { paddingValues ->
            YearInReviewOnboardingContent(
                modifier = modifier
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            )
        }
    )
}

@Composable
fun YearInReviewOnboardingContent(
    modifier: Modifier = Modifier
) {
    val gifAspectRatio = 4f / 3f
    Column(
        verticalArrangement = Arrangement.Top,
        modifier = modifier
            .fillMaxHeight()
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(R.drawable.yir_puzzle_pinch)
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
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 8.dp),
                text = stringResource(R.string.year_in_review_get_started_headline),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                text = stringResource(R.string.year_in_review_get_started_bodytext),
                color = WikipediaTheme.colors.primaryColor,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(
                modifier = Modifier
                    .weight(1f)
            )
            Text(
                modifier = Modifier
                    .padding(top = 10.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.year_in_review_get_started_info),
                color = WikipediaTheme.colors.secondaryColor,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
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
                    .padding(horizontal = 10.dp)
            ) {
                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.paperColor,
                        contentColor = WikipediaTheme.colors.progressiveColor),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
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
                        .weight(1f)
                        .padding(start = 12.dp),
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
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        YearInReviewOnboardingScreen(
            onBackButtonClick = {},
            onGetStartedClick = {}
        )
    }
}
