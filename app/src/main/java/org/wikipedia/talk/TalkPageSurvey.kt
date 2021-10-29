package org.wikipedia.talk

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.toSpannable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil

object TalkPageSurvey {

    fun shouldShowSurvey(): Boolean {
        return Prefs.talkPageSurveyOverride && fallsWithinGeoRange()
    }

    private fun fallsWithinGeoRange(): Boolean {
        val languages = WikipediaApp.getInstance().language().appLanguageCodes
        val country = GeoUtil.geoIPCountry.orEmpty()
        return (languages.contains("hi") ||
                languages.contains("id") ||
                languages.contains("ja") ||
                ((languages.contains("ar") || languages.contains("fr")) && (country == "MA" || country == "EG" || country == "ML" || country == "CD")) ||
                (languages.contains("en") && (country == "IN" || country == "NG")))
    }

    fun showSurvey(activity: Activity) {
        if (Prefs.showTalkPageSurvey) {
            showSurveyDialog(activity, true)
            Prefs.showTalkPageSurvey = false
        } else if (Prefs.showTalkPageSurveyLastAttempt) {
            showSurveyDialog(activity, false)
            Prefs.showTalkPageSurveyLastAttempt = false
        }
    }

    private fun showSurveyDialog(activity: Activity, isFirstAttempt: Boolean) {
        val dialog: AlertDialog = AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.survey_dialog_title))
            .setMessage(StringUtil.fromHtml(activity.getString(R.string.survey_dialog_message)).toSpannable())
            .setPositiveButton(R.string.survey_dialog_primary_action_text) { _, _ -> takeUserToSurvey(activity) }
            .setNegativeButton(if (isFirstAttempt) R.string.survey_dialog_neutral_action_text else R.string.dialog_message_edit_failed_cancel) { _, _ -> setReminderToShowSurveyLater(isFirstAttempt) }
            .setCancelable(false)
            .create()
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { url ->
                CustomTabsUtil.openInCustomTab(activity, url)
            }
    }

    private fun setReminderToShowSurveyLater(isFirstAttempt: Boolean) {
        Prefs.showTalkPageSurveyLastAttempt = isFirstAttempt
    }

    private fun takeUserToSurvey(context: Context) {
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        return when (WikipediaApp.getInstance().language().appLanguageCode) {
            "ar" -> "https://docs.google.com/forms/d/e/1FAIpQLScEeo2yy3xrJTx2iRn4DjnzGlCftyaGVoPHbg7Vp4TBNM2C1g/viewform"
            "fr" -> "https://docs.google.com/forms/d/e/1FAIpQLSeKsILKIKzdgzrwZjHPmThSKLbf5ZlN2vPH-iCgCioete94eA/viewform"
            "hi" -> "https://docs.google.com/forms/d/e/1FAIpQLSeYSHaa5Gd3PPDi0g14suUC-6LiKsQnyIFB-v8mA_3aCW73mA/viewform"
            "id" -> "https://docs.google.com/forms/d/e/1FAIpQLSeL9LTtHMJDdUevJYXp3Z5NS1QF2j7SovTX6RaCHbcIFcOPfA/viewform"
            "ja" -> "https://docs.google.com/forms/d/e/1FAIpQLSf3HTuDp9IGClMAoh2YhcyhtRfNj8GCuRavGyoGi5OC46ElCw/viewform"
            else -> "https://docs.google.com/forms/d/e/1FAIpQLSeF-G-QNaDUSGGx8LTCD92xbALq2KvZTDpN9NavCVZtajiOew/viewform"
        }
    }
}
