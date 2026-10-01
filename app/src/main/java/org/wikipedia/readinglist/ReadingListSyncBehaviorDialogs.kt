package org.wikipedia.readinglist

import android.app.Activity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.RecommendedReadingListEvent
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.databinding.DialogWithCheckboxBinding
import org.wikipedia.events.ReadingListsEnableSyncStatusEvent
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity.Companion.newIntent
import org.wikipedia.util.FeedbackUtil.showAndroidAppFAQ
import org.wikipedia.util.StringUtil

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

    fun promptEnableSyncDialog(activity: Activity) {
        if (!Prefs.showReadingListSyncEnablePrompt || Prefs.isSuggestedEditsHighestPriorityEnabled) {
            return
        }
        RecommendedReadingListEvent.submit("impression", "sync_enable_prompt")
        val binding = DialogWithCheckboxBinding.inflate(activity.layoutInflater)
        binding.dialogMessage.text = StringUtil.fromHtml(activity.getString(R.string.reading_list_prompt_turned_sync_on_dialog_text))
        binding.dialogMessage.movementMethod = LinkMovementMethodExt { _ -> showAndroidAppFAQ(activity) }
        binding.dialogCheckbox.setOnClickListener {
            RecommendedReadingListEvent.submit("noshow_click", "sync_enable_prompt")
        }
        MaterialAlertDialogBuilder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_prompt_turned_sync_on_dialog_title)
                .setView(binding.root)
                .setPositiveButton(R.string.reading_list_prompt_turned_sync_on_dialog_enable_syncing) { _, _ ->
                    RecommendedReadingListEvent.submit("sync_enable_click", "sync_enable_prompt")
                    ReadingListSyncAdapter.setSyncEnabledWithSetup()
                }
                .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks) { _, _ ->
                    RecommendedReadingListEvent.submit("nothanks_click", "sync_enable_prompt")
                }
                .setOnDismissListener {
                    Prefs.showReadingListSyncEnablePrompt = !binding.dialogCheckbox.isChecked
                    FlowEventBus.post(ReadingListsEnableSyncStatusEvent())
                }
                .show()
    }
}
