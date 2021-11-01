package org.wikipedia.talk

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil

object TalkPageSurvey {

    fun maybeShowSurvey(activity: Activity) {
        if (!activity.isDestroyed && (fallsWithinGeoRange() || Prefs.talkPageSurveyOverride)) {
            showSurveyDialog(activity)
        }
    }

    fun showSurveyDialog(activity: Activity) {
        val attempts = Prefs.showTalkPageSurveyAttempts
        if (attempts > 1) {
            return
        }

        Prefs.showTalkPageSurveyAttempts = attempts + 1

        val dialog = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.survey_dialog_title))
                .setMessage(StringUtil.fromHtml(activity.getString(R.string.talk_snackbar_survey_text) +
                        "<br/><br/><small><a href=\"https://foundation.m.wikimedia.org/wiki/Legal:Wikipedia_Android_App_Talk_Page_Survey_Privacy_Statement\">" +
                        activity.getString(R.string.privacy_policy_description) + "</a></small>"))
                .setPositiveButton(R.string.talk_snackbar_survey_action_text) { _, _ -> takeUserToSurvey(activity) }
                .setNegativeButton(if (attempts == 0) R.string.onboarding_maybe_later else android.R.string.cancel, null)
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { url ->
            CustomTabsUtil.openInCustomTab(activity, url)
        }
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

    private fun takeUserToSurvey(context: Context) {
        Prefs.showTalkPageSurveyAttempts = 10
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
