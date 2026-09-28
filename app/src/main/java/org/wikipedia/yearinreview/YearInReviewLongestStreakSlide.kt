package org.wikipedia.yearinreview

import android.icu.text.DateIntervalFormat
import android.icu.text.DateFormat
import android.icu.util.DateInterval
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
import java.time.LocalDate
import java.time.ZoneId

private const val PROPERTY_HEADLINE_START = "headline1"
private const val PROPERTY_STREAK_DAYS = "streakNumber"
private const val PROPERTY_HEADLINE_END = "headline2"
private const val PROPERTY_BODY_COPY = "bodyCopy"

val LongestStreakRiveSpec = RiveSlideSpec(
    resourceId = R.raw.autolayout_multiple_instances_test,
    artboardName = "frame2",
    stateMachineName = "insightFrame-stateMachine",
    viewModelName = "Frame2",
    instanceType = RiveInstanceType.Named("frame")
)

@Composable
fun YearInReviewLongestStreakSlide(
    riveFileResult: Result<RiveFile>,
    streakDays: Int,
    streakStartDate: LocalDate,
    streakEndDate: LocalDate,
    slideId: String,
    screenshotGetters: MutableMap<String, GetBitmapFun>,
    playing: Boolean,
    onRiveError: (Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    // e.g. "March 4 – 14" in English, collapsing the shared month the way the locale expects
    val streakDateRange = remember(locale, streakStartDate, streakEndDate) {
        val zone = ZoneId.systemDefault()
        DateIntervalFormat.getInstance(DateFormat.MONTH_DAY, locale).format(
            DateInterval(
                streakStartDate.atStartOfDay(zone).toInstant().toEpochMilli(),
                streakEndDate.atStartOfDay(zone).toInstant().toEpochMilli()
            )
        )
    }
    val textProperties = mapOf(
        PROPERTY_HEADLINE_START to stringResource(R.string.year_in_review_longest_streak_headline_start),
        PROPERTY_STREAK_DAYS to rememberLocalizedNumber(streakDays),
        PROPERTY_HEADLINE_END to pluralStringResource(R.plurals.year_in_review_longest_streak_headline_end, streakDays),
        PROPERTY_BODY_COPY to stringResource(R.string.year_in_review_longest_streak_body, streakDateRange)
    )
    YearInReviewRiveSlide(
        riveFileResult = riveFileResult,
        slideId = slideId,
        screenshotGetters = screenshotGetters,
        spec = LongestStreakRiveSpec,
        textProperties = textProperties,
        accessibilityDescription = textProperties.values.filter { it.isNotEmpty() }.joinToString(" "),
        playing = playing,
        modifier = modifier,
        onRiveError = onRiveError
    )
}
