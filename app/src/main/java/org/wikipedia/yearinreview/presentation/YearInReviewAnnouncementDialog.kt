package org.wikipedia.yearinreview.presentation

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.rive.Result
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.YearInReviewEvent
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.components.AppButton
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.UriUtil

class YearInReviewAnnouncementDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeYearInReviewRive(requireContext())
        if (savedInstanceState == null) {
            YearInReviewEvent.submit(action = "impression", slide = ANALYTICS_SLIDE)
            Prefs.yearInReviewVisited = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    YearInReviewAnnouncementScreen(
                        onCloseClick = {
                            YearInReviewEvent.submit(action = "close_click", slide = ANALYTICS_SLIDE)
                            showGetStartedLaterMessage()
                            dismiss()
                        },
                        onLearnMoreClick = {
                            UriUtil.visitInExternalBrowser(requireContext(), getString(R.string.year_in_review_reading_list_learn_more).toUri())
                        },
                        onShareFeedbackClick = {
                            FeedbackUtil.composeEmail(requireContext(), subject = getString(R.string.year_in_review_feedback_email_subject))
                        },
                        onExploreClick = {
                            YearInReviewEvent.submit(action = "continue_click", slide = ANALYTICS_SLIDE)
                            startActivity(YearInReviewActivity.newIntent(requireContext()))
                            dismiss()
                        }
                    ) {
                        YearInReviewAnnouncementCover()
                    }
                }
            }
        }
    }

    // Back, swipe-down and tapping outside the sheet
    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        showGetStartedLaterMessage()
    }

    private fun showGetStartedLaterMessage() {
        FeedbackUtil.showMessage(requireActivity(), getString(R.string.year_in_review_get_started_later))
    }

    companion object {
        // Same slide name as the full-screen onboarding prompt this sheet replaces, so its analytics stay comparable
        private const val ANALYTICS_SLIDE = "explore_prompt"

        fun newInstance() = YearInReviewAnnouncementDialog()
    }
}

@Composable
private fun YearInReviewAnnouncementCover() {
    val spec = YearInReviewPage.Cover.riveSpec ?: return
    // If Rive fails, the cover's placeholder background stays in place and Explore still works, so errors are only logged
    val riveWorker = rememberYearInReviewRiveWorker(onRiveError = {})
    val riveFontsResult = rememberYearInReviewRiveFonts(riveWorker, YearInReviewRiveFonts)
    val riveFiles = rememberYearInReviewRiveFiles(
        riveWorker = riveWorker,
        resourceIds = listOf(spec.resourceId),
        riveFontsResult = riveFontsResult
    )
    InstallRiveSystemFontFallback()
    YearInReviewRiveSlide(
        riveFileResult = riveFiles[spec.resourceId] ?: Result.Loading,
        spec = spec,
        // TODO: pass the user's own text once the cover carries it; the spec's instance shows the designer's sample text until then
        textProperties = emptyMap(),
        accessibilityDescription = stringResource(R.string.year_in_review_get_started_headline),
        playing = true,
        onRiveError = {}
    )
}

@Composable
fun YearInReviewAnnouncementScreen(
    onCloseClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onShareFeedbackClick: () -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverContent: @Composable BoxScope.() -> Unit
) {
    val coverHeight = (LocalConfiguration.current.screenHeightDp * COVER_HEIGHT_FRACTION).dp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius))
            .background(WikipediaTheme.colors.paperColor)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverHeight)
                .placeholderBackground()
        ) {
            coverContent()
            YearInReviewTopBar(
                iconColor = ComposeColors.White,
                onCloseClick = onCloseClick,
                onLearnMoreClick = onLearnMoreClick,
                onShareFeedbackClick = onShareFeedbackClick,
                showWikipediaLogo = false,
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
        AppButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = onExploreClick
        ) {
            Text(text = stringResource(R.string.year_in_review_announcement_explore))
        }
    }
}

private const val COVER_HEIGHT_FRACTION = 0.7f
// Matches the top corners of the Material 3 modal bottom sheet that hosts this screen
private val SheetCornerRadius = 28.dp

@Preview
@Composable
private fun YearInReviewAnnouncementScreenPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        YearInReviewAnnouncementScreen(
            onCloseClick = {},
            onLearnMoreClick = {},
            onShareFeedbackClick = {},
            onExploreClick = {},
            coverContent = {}
        )
    }
}
