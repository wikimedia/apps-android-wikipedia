package org.wikipedia.widgets.readingchallenge

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import org.wikipedia.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

data class WidgetCombination(
    val iconResId: Int,
    val backgroundColor: Color,
    val contentColor: Color,
    val progressColor: Color? = null,
    val titleResId: Int? = null,
    val subtitleResId: Int? = null
)

object WidgetCombinations {

    // TODO: update iconResId when svg's are provided
    val streakNeedsReading = listOf(
        needsReadingCombination(textResId = R.string.reading_challenge_widget_reminder_dont_let_today_drift, iconResId = R.drawable.wp25_babyglobe_sleeping),
        needsReadingCombination(textResId = R.string.reading_challenge_widget_reminder_before_day_snoozes, iconResId = R.drawable.wp25_babyglobe_sleeping),
        needsReadingCombination(R.string.reading_challenge_widget_reminder_small_bit_counts, iconResId = R.drawable.wp25_babyglobe_sleeping),
        needsReadingCombination(R.string.reading_challenge_widget_reminder_one_article_away, iconResId = R.drawable.wp25_babyglobe_sleeping),
        needsReadingCombination(R.string.reading_challenge_widget_reminder_jump_in_anytime),
        needsReadingCombination(R.string.reading_challenge_widget_reminder_quiet_reading_moment),
        needsReadingCombination(R.string.reading_challenge_widget_reminder_keep_curiosity_going),
        needsReadingCombination(R.string.reading_challenge_widget_reminder_one_article_away)
    )

    val enrolledNotStarted = listOf(
        needsEnrolledNotStartedCombination(
            titleResId = R.string.reading_challenge_widget_enrolled_not_started_title,
            subtitleResId = R.string.reading_challenge_widget_enrolled_not_started_subtitle
        ),
        needsEnrolledNotStartedCombination(
            titleResId = R.string.reading_challenge_widget_start_reading_challenge_title,
            subtitleResId = R.string.reading_challenge_widget_start_reading_challenge_subtitle
        ),
        needsEnrolledNotStartedCombination(
            titleResId = R.string.reading_challenge_widget_start_spin_up_new_streak_title,
            subtitleResId = R.string.reading_challenge_widget_start_spin_up_new_streak_subtitle
        ),
        needsEnrolledNotStartedCombination(
            titleResId = R.string.reading_challenge_widget_start_fresh_start_title,
            subtitleResId = R.string.reading_challenge_widget_start_fresh_start_subtitle
        )
    )

    // Since we are showing progress bar the title from the doc is not required
    val streakOngoing = listOf(
        WidgetCombination(
            iconResId = R.drawable.wp25_babyglobe_phone,
            backgroundColor = WidgetColors.phoneReadingBackground,
            contentColor = WidgetColors.phoneReadingContent,
            progressColor = WidgetColors.phoneReadingProgressColor
        ),
        WidgetCombination(
            iconResId = R.drawable.wp25_babyglobe_music,
            backgroundColor = WidgetColors.musicReadingBackground,
            contentColor = WidgetColors.musicContent,
            progressColor = WidgetColors.musicReadingProgressColor
        ),
        WidgetCombination(
            iconResId = R.drawable.wp25_babyglobe_space,
            backgroundColor = WidgetColors.spaceReadingBackground,
            contentColor = WidgetColors.spaceContent,
            progressColor = WidgetColors.spaceReadingProgressColor
        )
    )

    fun List<WidgetCombination>.forToday(
        enrollmentDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): WidgetCombination {
        val daysSinceEnrollment = DAYS.between(enrollmentDate, today).coerceAtLeast(0)
        val index = (daysSinceEnrollment % this.size).toInt()
        return this[index]
    }

    private fun needsReadingCombination(@StringRes textResId: Int, @DrawableRes iconResId: Int = R.drawable.wp25_babyglobe_reading) =
        WidgetCombination(
            iconResId = iconResId,
            backgroundColor = WidgetColors.streakOngoingNeedsReadingBackground,
            contentColor = WidgetColors.streakOngoingNeedsReadingContent,
            titleResId = textResId
        )

    private fun needsEnrolledNotStartedCombination(
        @StringRes titleResId: Int,
        @StringRes subtitleResId: Int,
        @DrawableRes iconResId: Int = R.drawable.wp25_babyglobe_reading,
    ) =
        WidgetCombination(
            iconResId = iconResId,
            backgroundColor = WidgetColors.challengeNotOptInBackground,
            contentColor = WidgetColors.primary,
            titleResId = titleResId,
            subtitleResId = subtitleResId
        )
}
