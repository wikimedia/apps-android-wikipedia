package org.wikipedia.onboarding

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
import java.util.*

object OnboardingSurveyHelper {
    private const val privacyPolicyUrl = "https://foundation.wikimedia.org/wiki/Legal:Wikipedia_Android_App_Onboarding_Survey_Privacy_Statement"

    fun maybeShowSurvey(activity: Activity) {
        if (!Prefs.isOnboardingSurveyShown && fallsWithinGeoRange() && fallsWithinDateRange()) {
            showSurveyDialog(activity)
        }
    }

    private fun showSurveyDialog(activity: Activity) {
        if (activity.isDestroyed) {
            return
        }
        val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.onboarding_survey_title)
                .setMessage(StringUtil.fromHtml(activity.getString(R.string.onboarding_survey_body) +
                        "<br/><br/><small><a href=\"$privacyPolicyUrl\">" +
                        activity.getString(R.string.privacy_policy_description) + "</a></small>"))
                .setPositiveButton(R.string.suggested_edits_snackbar_survey_action_text) { _, _ -> takeUserToSurvey(activity) }
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { url ->
            CustomTabsUtil.openInCustomTab(activity, url)
        }
        Prefs.isOnboardingSurveyShown = true
    }

    private fun fallsWithinGeoRange(): Boolean {
        val languages = WikipediaApp.instance.languageState.appLanguageCodes
        val country = GeoUtil.geoIPCountry.orEmpty()
        return (languages.contains("hi") ||
                languages.contains("id") ||
                languages.contains("ja") ||
                ((languages.contains("ar") || languages.contains("fr")) && (country == "MA" || country == "EG" || country == "ML" || country == "CD")))
    }

    private fun fallsWithinDateRange(): Boolean {
        val endTime = GregorianCalendar(2022, 6, 22)
        return Calendar.getInstance().timeInMillis < endTime.timeInMillis
    }

    private fun takeUserToSurvey(context: Context) {
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        return when (WikipediaApp.instance.languageState.appLanguageCode) {
            "ar" -> "https://docs.google.com/forms/d/e/1FAIpQLSd1_lGJbzhWoynf1zg72NlxdlbO5g9Bc9IpFolSsddlgOSIlA/viewform"
            "fr" -> "https://docs.google.com/forms/d/e/1FAIpQLSccqK5NQrXE01BeXkcNvvPtvxhivJpybADQQGzSozG6bWUAqA/viewform"
            "hi" -> "https://docs.google.com/forms/d/e/1FAIpQLSdezeODPzZvNeEtVkztU3AuDgfB16lFJ2BM1i4s6XlSZaxEgw/viewform"
            "id" -> "https://docs.google.com/forms/d/e/1FAIpQLScRxd-t954xu7pBOkA5GxJ2SbkDk7rHvR_m4zlp3ewL0EhoMQ/viewform"
            "ja" -> "https://docs.google.com/forms/d/e/1FAIpQLScDlBWHOH77-_s9lK481bYS85wZDVDX-k3IUF7rcT2ikb01PA/viewform"
            else -> "https://docs.google.com/forms/d/e/1FAIpQLScPxXKBpT-CHq4kQy6LuOMjCIjHH7C3NXmyJKrT8K-pqTew4Q/viewform"
        }
    }
}
