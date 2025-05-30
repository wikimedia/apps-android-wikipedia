package org.wikipedia.readinglist.recommended

import androidx.annotation.StringRes
import org.wikipedia.R

enum class RecommendedReadingListUpdateFrequency(
    @StringRes val displayStringRes: Int,
    @StringRes val dialogStringRes: Int,
    @StringRes val snackbarStringRes: Int
) {
    DAILY(
        R.string.recommended_reading_list_settings_updates_frequency_daily,
        R.string.recommended_reading_list_settings_updates_frequency_dialog_daily,
        R.string.recommended_reading_list_page_snackbar_day
    ),
    WEEKLY(
        R.string.recommended_reading_list_settings_updates_frequency_weekly,
        R.string.recommended_reading_list_settings_updates_frequency_dialog_weekly,
        R.string.recommended_reading_list_page_snackbar_week
    ),
    MONTHLY(
        R.string.recommended_reading_list_settings_updates_frequency_monthly,
        R.string.recommended_reading_list_settings_updates_frequency_dialog_monthly,
        R.string.recommended_reading_list_page_snackbar_month
    )
}

enum class RecommendedReadingListSource {
    INTERESTS,
    READING_LIST,
    HISTORY
}
