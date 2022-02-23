package org.wikipedia.readinglist

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.databinding.DialogWithCheckboxBinding
import org.wikipedia.events.ReadingListsEnableSyncStatusEvent
import org.wikipedia.login.LoginActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.SettingsActivity.Companion.newIntent
import org.wikipedia.util.FeedbackUtil.showAndroidAppFAQ
import org.wikipedia.util.StringUtil

object ReadingListSyncBehaviorDialogs {
    private var PROMPT_LOGIN_TO_SYNC_DIALOG_SHOWING = false

    fun detectedRemoteTornDownDialog(activity: Activity) {
        AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_turned_sync_off_dialog_title)
                .setMessage(R.string.reading_list_turned_sync_off_dialog_text)
                .setPositiveButton(R.string.reading_list_turned_sync_off_dialog_ok, null)
                .setNegativeButton(R.string.reading_list_turned_sync_off_dialog_settings) { _, _ -> activity.startActivity(newIntent(activity)) }
                .show()
    }

    fun promptEnableSyncDialog(activity: Activity) {
        if (!Prefs.showReadingListSyncEnablePrompt || Prefs.isSuggestedEditsHighestPriorityEnabled) {
            return
        }
        val binding = DialogWithCheckboxBinding.inflate(activity.layoutInflater)
        binding.dialogMessage.text = StringUtil.fromHtml(activity.getString(R.string.reading_list_prompt_turned_sync_on_dialog_text))
        binding.dialogMessage.movementMethod = LinkMovementMethodExt { _ -> showAndroidAppFAQ(activity) }
        AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_prompt_turned_sync_on_dialog_title)
                .setView(binding.root)
                .setPositiveButton(R.string.reading_list_prompt_turned_sync_on_dialog_enable_syncing) { _, _ -> ReadingListSyncAdapter.setSyncEnabledWithSetup() }
                .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks, null)
                .setOnDismissListener {
                    Prefs.showReadingListSyncEnablePrompt = !binding.dialogCheckbox.isChecked
                    WikipediaApp.getInstance().bus.post(ReadingListsEnableSyncStatusEvent())
                }
                .show()
    }

    fun promptLogInToSyncDialog(activity: Activity) {
        if (!Prefs.showReadingListSyncEnablePrompt || PROMPT_LOGIN_TO_SYNC_DIALOG_SHOWING) {
            return
        }
        val binding = DialogWithCheckboxBinding.inflate(activity.layoutInflater)
        binding.dialogMessage.text = StringUtil.fromHtml(activity.getString(R.string.reading_lists_login_reminder_text_with_link))
        binding.dialogMessage.movementMethod = LinkMovementMethodExt { _ -> showAndroidAppFAQ(activity) }
        AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.reading_list_login_reminder_title)
                .setView(binding.root)
                .setPositiveButton(R.string.reading_list_preference_login_or_signup_to_enable_sync_dialog_login) { _, _ ->
                    val loginIntent = LoginActivity.newIntent(activity, LoginFunnel.SOURCE_READING_MANUAL_SYNC)
                    activity.startActivity(loginIntent)
                }
                .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks, null)
                .setOnDismissListener {
                    PROMPT_LOGIN_TO_SYNC_DIALOG_SHOWING = false
                    Prefs.showReadingListSyncEnablePrompt = !binding.dialogCheckbox.isChecked
                }
                .show()
        PROMPT_LOGIN_TO_SYNC_DIALOG_SHOWING = true
    }
}
