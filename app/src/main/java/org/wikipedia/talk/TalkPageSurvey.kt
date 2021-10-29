package org.wikipedia.talk

import android.content.Context
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil

class TalkPageSurvey {

    companion object {

        fun showSurveyFirstAttempt(context: Context) {
            if (Prefs.showTalkPageSurvey) {
                showSurvey(context, true)
                Prefs.showTalkPageSurvey = false
            }
        }

        fun showSurveyLastAttempt(context: Context) {
            if (Prefs.showTalkPageSurveyLastAttempt) {
                showSurvey(context, false)
                Prefs.showTalkPageSurveyLastAttempt = false
            }
        }

        private fun showSurvey(context: Context, isFirstAttempt: Boolean) {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.survey_dialog_title))
                .setMessage(context.getString(R.string.survey_dialog_message))
                .setPositiveButton(R.string.survey_dialog_primary_action_text) { _, _ -> takeUserToSurvey(context) }
                .setNegativeButton(R.string.survey_dialog_secondary_action_text) { _, _ -> takeUserToPrivacyPolicy(context) }
                .setNeutralButton(if (isFirstAttempt)R.string.survey_dialog_neutral_action_text else R.string.dialog_message_edit_failed_cancel) { _, _ -> setReminderToShowSurveyLater(isFirstAttempt) }
                .setCancelable(false)
                .create()
                .show()
        }

        private fun setReminderToShowSurveyLater(isFirstAttempt: Boolean) {
            Prefs.showTalkPageSurveyLastAttempt = isFirstAttempt
        }

        private fun takeUserToSurvey(context: Context) {
            CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
        }

        private fun getLanguageSpecificUrl(): String {
            return when (WikipediaApp.getInstance().language().appLanguageCode) {
                "ar" -> "https://forms.gle/TtVov1y2ggkSU5jz6v"
                "fr" -> "https://forms.gle/WxRP7NSUttyQUQgs7"
                "hi" -> ""
                "id" -> "https://forms.gle/8EgVWERw4FnqGSVv7"
                "ja" -> ""
                else -> "https://forms.gle/8W2kYaRgADEL6cTG8"
            }
        }

        private fun takeUserToPrivacyPolicy(context: Context) {
            CustomTabsUtil.openInCustomTab(context, "https://foundation.wikimedia.org/wiki/Legal:Wikipedia_Android_App_Talk_Page_Survey_Privacy_Statement")
        }
    }
}
