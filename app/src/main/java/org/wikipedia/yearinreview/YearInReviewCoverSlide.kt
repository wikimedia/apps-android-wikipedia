package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.rive.GetBitmapFun
import app.rive.Result
import app.rive.RiveFile
import org.wikipedia.R

private const val PROPERTY_COVER_TITLE = "coverTitle"
private const val PROPERTY_BODY_COPY = "bodyCopy"

val CoverRiveSpec = RiveSlideSpec(
    resourceId = R.raw.all_templates,
    artboardName = "cover",
    stateMachineName = "cover-statemachine",
    viewModelName = "DataTemplate",
    instanceType = RiveInstanceType.Named("cover"),
    globalViewModel = YearInReviewRiveGlobalProperties
)

@Composable
fun YearInReviewCoverSlide(
    riveFileResult: Result<RiveFile>,
    year: Int,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>,
    playing: Boolean,
    onRiveError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    daysRead: Int
) {
    val locale = LocalConfiguration.current.locales[0]
    val coverTitle = stringResource(R.string.year_in_review_cover_title).uppercase(locale)
    val bodyCopy = pluralStringResource(R.plurals.year_in_review_cover_body, daysRead, rememberLocalizedNumber(daysRead), year)
    YearInReviewRiveSlide(
        riveFileResult = riveFileResult,
        slideId = slideId,
        screenshotGetters = screenshotGetters,
        spec = CoverRiveSpec,
        textProperties = mapOf(
            PROPERTY_COVER_TITLE to coverTitle,
            PROPERTY_BODY_COPY to bodyCopy
        ),
        accessibilityDescription = "$coverTitle $bodyCopy",
        playing = playing,
        modifier = modifier,
        onRiveError = onRiveError
    )
}
