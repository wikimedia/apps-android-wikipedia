package org.wikipedia.readinglist

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.settings.SettingsActivity.Companion.newIntent

object ReadingListSyncBehaviorDialogs {
    fun detectedRemoteTornDownDialog(activity: Activity) {
        RecommendedReadingListEvent.submit("impression", "sync_off_prompt")
        MaterialAlertDialogBuilder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_turned_sync_off_dialog_title)
                .setMessage(R.string.reading_list_turned_sync_off_dialog_text)
                .setPositiveButton(R.string.reading_list_turned_sync_off_dialog_ok) { _, _ ->
                    RecommendedReadingListEvent.submit("ok_click", "sync_off_prompt")
                }
                .setNegativeButton(R.string.reading_list_turned_sync_off_dialog_settings) { _, _ ->
                    RecommendedReadingListEvent.submit("settings_click", "sync_off_prompt")
                    activity.startActivity(newIntent(activity))
                }
                .show()
    }
}
