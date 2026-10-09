package org.wikipedia.yearinreview.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.rive.Result
import org.wikipedia.R
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.components.error.WikiErrorClickEvents
import org.wikipedia.compose.components.error.WikiErrorView
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import org.wikipedia.util.UiState
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewSnapshot

@Composable
fun YearInReviewOnboardingScreen(
    // The user's logged-in state, and their Year in Review snapshot
    uiState: UiState<Pair<Boolean, YearInReviewSnapshot>>,
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onAboutInsightsClick: () -> Unit,
    onShareFeedbackClick: () -> Unit,
    onExploreClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverContent: @Composable BoxScope.(snapshot: YearInReviewSnapshot, onCoverReady: () -> Unit) -> Unit
) {
    val isError = uiState is UiState.Error
    var isCoverReady by remember { mutableStateOf(false) }
    // The cover is drawn behind the system bars and the Explore button, so the button always sits on the same
    // background as the cover: the placeholder while Rive loads, and the Rive artboard once it's drawn
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (isError) Modifier else Modifier.placeholderBackground())
    ) {
        when (uiState) {
            UiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = WikipediaTheme.colors.progressiveColor
                )
            }
            is UiState.Success -> {
                coverContent(uiState.data.second) { isCoverReady = true }
                if (isCoverReady) {
                    AppButton(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = onExploreClick
                    ) {
                        Text(text = stringResource(R.string.year_in_review_announcement_explore))
                    }
                }
            }
            is UiState.Error -> {
                WikiErrorView(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    caught = uiState.error,
                    errorClickEvents = WikiErrorClickEvents(
                        retryClickListener = onRetryClick,
                        backClickListener = onCloseClick
                    ),
                    // The snapshot also fails without the remote config, which isn't a network error but is still worth retrying
                    retryForGenericError = true
                )
            }
        }
        // Pads itself below the status bar
        YearInReviewTopBar(
            // The error is shown on the paper color rather than over the cover's dark placeholder
            iconColor = if (isError) WikipediaTheme.colors.primaryColor else ComposeColors.White,
            onCloseClick = onCloseClick,
            onLearnMoreClick = onLearnMoreClick,
            onAboutInsightsClick = onAboutInsightsClick,
            onShareFeedbackClick = onShareFeedbackClick,
            showWikipediaLogo = false
        )
    }
}

@Composable
fun YearInReviewOnboardingCover(snapshot: YearInReviewSnapshot, onCoverReady: () -> Unit) {
    val resources = LocalResources.current
    val accessibilityDescription = stringResource(R.string.year_in_review_get_started_headline)
    val content = remember(resources, accessibilityDescription, snapshot) {
        val isPersonalized = YearInReviewFlowDecider.isDataRich(snapshot)
        val daysSpent = snapshot.readingStats.visitedDaysCount
        RiveSlideContent(
            spec = allTemplatesSlideSpec("cover"),
            textProperties = mapOf(
                "coverTitle" to resources.getString(if (isPersonalized) R.string.yir_intro_headline else R.string.yir_community_intro_headline),
                "bodyCopy" to if (isPersonalized) {
                    resources.getQuantityString(R.plurals.yir_intro_supporting, daysSpent, daysSpent, YearInReviewConfig.YEAR)
                } else {
                    resources.getString(R.string.yir_community_intro_supporting)
                }
            ),
            accessibilityDescription = accessibilityDescription
        )
    }
    // If Rive fails, the cover's placeholder background stays in place and Explore is still shown
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
        onRiveLoaded = onCoverReady,
        onRiveError = {}
    )
}

@Preview
@Composable
private fun YearInReviewOnboardingScreenLoadingPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewOnboardingScreen(
            uiState = UiState.Loading,
            onCloseClick = {},
            onLearnMoreClick = {},
            onAboutInsightsClick = {},
            onShareFeedbackClick = {},
            onExploreClick = {},
            onRetryClick = {},
            coverContent = { _, _ -> }
        )
    }
}
