package org.wikipedia.readinglist.recommended

import androidx.annotation.StringRes
import org.wikipedia.R

enum class RecommendedReadingListUpdateFrequency(
    @StringRes val displayStringRes: Int,
    @StringRes val dialogStringRes: Int
) {
    DAILY(
        R.string.recommended_reading_list_settings_updates_frequency_daily,
        R.string.recommended_reading_list_settings_updates_frequency_dialog_daily
    ),
    WEEKLY(
        R.string.recommended_reading_list_settings_updates_frequency_weekly,
        R.string.recommended_reading_list_settings_updates_frequency_dialog_weekly
    ),
    MONTHLY(
        R.string.recommended_reading_list_settings_updates_frequency_monthly,
        R.string.recommended_reading_list_settings_updates_frequency_dialog_monthly
    )
}

enum class RecommendedReadingListSource(
    @StringRes val type: Int
) {
    INTERESTS(
        R.string.recommended_reading_list_settings_updates_base_subtitle_interests
    ),
    READING_LIST(
        R.string.recommended_reading_list_settings_updates_base_subtitle_saved
    ),
    HISTORY(
        R.string.recommended_reading_list_settings_updates_base_subtitle_history
    )
}
