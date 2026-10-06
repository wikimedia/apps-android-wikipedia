package org.wikipedia.yearinreview.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rive.Result
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState

@Composable
fun YearInReviewOnboardingScreen(
    uiState: UiState<Boolean>,
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onShareFeedbackClick: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverContent: @Composable BoxScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WikipediaTheme.colors.paperColor)
            .navigationBarsPadding()
    ) {
        // Drawn behind the status bar, which the top bar pads itself below
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .placeholderBackground()
        ) {
            coverContent()
            YearInReviewTopBar(
                iconColor = ComposeColors.White,
                onCloseClick = onCloseClick,
                onLearnMoreClick = onLearnMoreClick,
                onShareFeedbackClick = onShareFeedbackClick,
                showWikipediaLogo = false
            )
        }
        AppButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // Waits for the login check, so a stale session is caught before choosing the logged-in or logged-out flow
            enabled = uiState !is UiState.Loading,
            onClick = onExploreClick
        ) {
            Text(text = stringResource(R.string.year_in_review_announcement_explore))
        }
    }
}

@Composable
fun YearInReviewOnboardingCover() {
    val resources = LocalResources.current
    val accessibilityDescription = stringResource(R.string.year_in_review_get_started_headline)
    val content = remember(resources, accessibilityDescription) {
        YearInReviewRiveContentMapper.map(YearInReviewPage.Cover, resources)?.copy(accessibilityDescription = accessibilityDescription)
    } ?: return
    // If Rive fails, the cover's placeholder background stays in place and Explore still works, so errors are only logged
    val riveWorker = rememberYearInReviewRiveWorker(onRiveError = {})
    val riveFontsResult = rememberYearInReviewRiveFonts(riveWorker, YearInReviewRiveFonts)
    val riveFiles = rememberYearInReviewRiveFiles(
        riveWorker = riveWorker,
        resourceIds = listOf(content.spec.resourceId),
        riveFontsResult = riveFontsResult
    )
    InstallRiveSystemFontFallback()
    YearInReviewRiveSlide(
        riveFileResult = riveFiles[content.spec.resourceId] ?: Result.Loading,
        content = content,
        playing = true,
        onRiveError = {}
    )
}

@Preview
@Composable
private fun YearInReviewOnboardingScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewOnboardingScreen(
            uiState = UiState.Success(false),
            onCloseClick = {},
            onLearnMoreClick = {},
            onShareFeedbackClick = {},
            onExploreClick = {},
            coverContent = {}
        )
    }
}
