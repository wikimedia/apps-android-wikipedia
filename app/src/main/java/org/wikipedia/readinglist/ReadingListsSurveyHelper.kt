package org.wikipedia.readinglist

import android.app.Activity
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ReadingListsFunnel
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.StringUtil
import java.util.*

object ReadingListsSurveyHelper {
    private const val MODE_INACTIVE = 0
    private const val MODE_ACTIVE = 1
    private const val MODE_OVERRIDE = 2

    fun activateSurvey() {
        if (!isActive()) {
            Prefs.readingListShareSurveyMode = MODE_ACTIVE
        }
    }

    fun maybeShowSurvey(activity: Activity) {
        if (!activity.isDestroyed && (Prefs.readingListShareSurveyMode == MODE_OVERRIDE ||
                        (isActive() && ReadingListsShareHelper.shareEnabled() && fallsWithinDateRange()))) {
            showSurveyDialog(activity)
        }
    }

    private fun showSurveyDialog(activity: Activity) {
        val attempts = Prefs.readingListShareSurveyAttempts
        if (attempts > 1) {
            return
        }
        Prefs.readingListShareSurveyAttempts = attempts + 1

        val dialog = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.reading_list_share_survey_title))
                .setMessage(StringUtil.fromHtml(activity.getString(R.string.reading_list_share_survey_body) +
                        "<br/><br/><small><a href=\"${activity.getString(R.string.survey_privacy_policy_url)}\">" +
                        activity.getString(R.string.privacy_policy_description) + "</a></small>"))
                .setPositiveButton(R.string.talk_snackbar_survey_action_text) { _, _ -> takeUserToSurvey(activity) }
                .setNegativeButton(if (attempts == 0) R.string.onboarding_maybe_later else android.R.string.cancel, null)
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { url ->
            CustomTabsUtil.openInCustomTab(activity, url)
        }
        ReadingListsFunnel().logSurveyShown()
    }

    private fun isActive(): Boolean {
        return Prefs.readingListShareSurveyMode != MODE_INACTIVE
    }

    private fun fallsWithinGeoRange(): Boolean {
        val languages = WikipediaApp.instance.languageState.appLanguageCodes
        val country = GeoUtil.geoIPCountry.orEmpty()
        return (languages.contains("hi") ||
                languages.contains("id") ||
                languages.contains("ja") ||
                ((languages.contains("ar") || languages.contains("fr")) && (country == "MA" || country == "EG" || country == "ML" || country == "CD")) ||
                (languages.contains("en") && (country == "IN" || country == "NG")))
    }

    private fun fallsWithinDateRange(): Boolean {
        val endTime = GregorianCalendar(2022, 11, 1)
        return Calendar.getInstance().timeInMillis < endTime.timeInMillis
    }

    private fun takeUserToSurvey(context: Context) {
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        // TODO
        return when (WikipediaApp.instance.languageState.appLanguageCode) {
            "ar" -> ""
            "fr" -> ""
            "hi" -> ""
            "id" -> ""
            "ja" -> ""
            else -> ""
        }
    }
}
