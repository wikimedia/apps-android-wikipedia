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
import org.wikipedia.util.StringUtil
import java.util.*

object ReadingListsShareSurveyHelper {
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

    fun isActive(): Boolean {
        return Prefs.readingListShareSurveyMode != MODE_INACTIVE
    }

    private fun fallsWithinDateRange(): Boolean {
        val endTime = GregorianCalendar(2022, 11, 1)
        return Calendar.getInstance().timeInMillis < endTime.timeInMillis
    }

    private fun takeUserToSurvey(context: Context) {
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        return when (WikipediaApp.instance.languageState.appLanguageCode) {
            "ar" -> "https://docs.google.com/forms/d/15ZnQRm8J3UtAxkS0BSaq2jGraGeCd8ojKwt97xjlO4Y"
            "bn" -> "https://forms.gle/864rFuD19qETpSTv7"
            "fr" -> "https://forms.gle/FHNm9LZdQfkbUbW58"
            "de" -> "https://docs.google.com/forms/d/e/1FAIpQLSfS2-gQJtCUnFMJl-C0BdrWNxpb-PeXjoDeCR4z80gSCoA-RA/viewform?usp=sf_link"
            "hi" -> "https://forms.gle/bKYnrH2rAv6pZ8718"
            "pt" -> "https://docs.google.com/forms/d/e/1FAIpQLSfbRhbf-cqmZC-vn1S_OTdsJ0zpiVW7vfFpWQgZtzQbU0dZEw/viewform?usp=sf_link"
            else -> "https://docs.google.com/forms/d/1c7TyQc-Dr9RA7wi6tCAdmWegxNj-s2GpQ1Vk2II6xYY"
        }
    }
}
