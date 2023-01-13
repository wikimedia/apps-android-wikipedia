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
        if (shouldShowSurvey(activity)) {
            showSurveyDialog(activity)
        }
    }

    fun shouldShowSurvey(activity: Activity): Boolean {
        val attempts = Prefs.readingListShareSurveyAttempts
        return !activity.isDestroyed && attempts <= 1 &&
                (Prefs.readingListShareSurveyMode == MODE_OVERRIDE || (isActive() && ReadingListsShareHelper.shareEnabled() && fallsWithinDateRange()))
    }

    private fun showSurveyDialog(activity: Activity) {
        val attempts = Prefs.readingListShareSurveyAttempts
        Prefs.readingListShareSurveyAttempts = attempts + 1

        val dialog = AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.reading_list_share_survey_title))
                .setMessage(StringUtil.fromHtml(activity.getString(R.string.reading_list_share_survey_body) +
                        "<br/><br/><small><a href=\"${getLanguageSpecificPrivacyPolicyUrl()}\">" +
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

    private fun fallsWithinDateRange(): Boolean {
        val endTime = GregorianCalendar(2022, Calendar.DECEMBER, 30)
        return Calendar.getInstance().timeInMillis < endTime.timeInMillis
    }

    private fun takeUserToSurvey(context: Context) {
        Prefs.readingListShareSurveyAttempts = 10
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        return when (WikipediaApp.instance.languageState.appLanguageCode) {
            "ar" -> "https://docs.google.com/forms/d/e/1FAIpQLSdZaFN5hm76xsFuJWlrNT1VFfUV14T0Yg9uA0o11579GfPszg/viewform?usp=sf_link"
            "bn" -> "https://docs.google.com/forms/d/e/1FAIpQLSeR_K5IYCQhuLgA6CCUdaSY71m6T7H0TiVaZ8rJ4nSYlUVCqA/viewform?usp=sf_link"
            "fr" -> "https://docs.google.com/forms/d/e/1FAIpQLSdKUCL5zAzsa87cKpcxZjmnzFc2NhaCH9W2Xn6DdXRZTwZ-0g/viewform?usp=sf_link"
            "de" -> "https://docs.google.com/forms/d/e/1FAIpQLSf6zbrkwe7lVLJtKBJBkjlLxjcpXtHVKMeUHF_POgMJsFAPLA/viewform?usp=sf_link"
            "hi" -> "https://docs.google.com/forms/d/e/1FAIpQLSdEtYzoNsmztbk05NtH82c3GDaEYn_-5aYdMa3NTO-FVWb_7A/viewform?usp=sf_link"
            "pt" -> "https://docs.google.com/forms/d/e/1FAIpQLSdwTvojzJRV1FT9apLXF9ck68Knq2qzVaaJQMbzaTvub_icWA/viewform?usp=sf_link"
            "es" -> "https://docs.google.com/forms/d/e/1FAIpQLScYsLE48ZjJynHhu6IgP6eR_PPuxjS78ejo_Ii9ysTfTxF9EQ/viewform?usp=sf_link"
            "ur" -> "https://docs.google.com/forms/d/e/1FAIpQLSfFdgf_Fr7slHsBanC8hzX34nN-R5nlP6_-DSjBdJHFYe8nng/viewform?usp=sf_link"
            else -> "https://docs.google.com/forms/d/e/1FAIpQLScnNlch1dLsxOdKU8oLupaTluW0pmXeNqMxdoX2pj6gJaOgVw/viewform?usp=sf_link"
        }
    }

    private fun getLanguageSpecificPrivacyPolicyUrl(): String {
        return when (WikipediaApp.instance.languageState.appLanguageCode) {
            "ar" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/ar"
            "bn" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/bn"
            "fr" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/fr"
            "de" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/de"
            "hi" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/hi"
            "pt" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/pt-br"
            "es" -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement/es"
            else -> "https://foundation.wikimedia.org/wiki/Legal:Feedback_form_for_sharing_reading_lists_Privacy_Statement"
        }
    }
}
