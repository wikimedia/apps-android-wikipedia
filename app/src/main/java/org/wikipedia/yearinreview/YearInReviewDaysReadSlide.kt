package org.wikipedia.yearinreview

import android.icu.text.DateFormatSymbols
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.rive.GetBitmapFun
import app.rive.Result
import app.rive.RiveFile
import org.wikipedia.R
import java.time.Month

private const val PROPERTY_HEADLINE_START = "headline1"
private const val PROPERTY_DAYS_READ = "readDays"
private const val PROPERTY_HEADLINE_END = "headline2"
private const val PROPERTY_BODY_COPY = "bodyCopy"

val DaysReadRiveSpec = RiveSlideSpec(
    resourceId = R.raw.autolayout_multiple_instances_test,
    artboardName = "frame1",
    stateMachineName = "insightFrame-stateMachine",
    viewModelName = "Frame1",
    instanceType = RiveInstanceType.Named("frame")
)

@Composable
fun YearInReviewDaysReadSlide(
    riveFileResult: Result<RiveFile>,
    year: Int,
    daysRead: Int,
    peakMonth: Month,
    peakMonthDaysRead: Int,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>,
    playing: Boolean,
    onRiveError: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    // Standalone form, so languages that inflect month names (e.g. Russian "декабрь", not "декабря") read correctly on their own
    val peakMonthName = remember(locale, peakMonth) {
        DateFormatSymbols.getInstance(locale).getMonths(DateFormatSymbols.STANDALONE, DateFormatSymbols.WIDE)[peakMonth.value - 1]
    }
    val textProperties = mapOf(
        PROPERTY_HEADLINE_START to stringResource(R.string.year_in_review_days_read_headline_start, year),
        PROPERTY_DAYS_READ to rememberLocalizedNumber(daysRead),
        PROPERTY_HEADLINE_END to pluralStringResource(R.plurals.year_in_review_days_read_headline_end, daysRead),
        PROPERTY_BODY_COPY to pluralStringResource(
            R.plurals.year_in_review_days_read_body,
            peakMonthDaysRead,
            peakMonthName,
            rememberLocalizedNumber(peakMonthDaysRead)
        )
    )
    YearInReviewRiveSlide(
        riveFileResult = riveFileResult,
        slideId = slideId,
        screenshotGetters = screenshotGetters,
        spec = DaysReadRiveSpec,
        textProperties = textProperties,
        accessibilityDescription = textProperties.values.filter { it.isNotEmpty() }.joinToString(" "),
        playing = playing,
        modifier = modifier,
        onRiveError = onRiveError
    )
}
