package org.wikipedia.readinglist

import android.app.Activity
import android.content.Context
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.ReadingListsAnalyticsHelper
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.StringUtil
import java.time.LocalDate
import java.time.Month

object ReadingListsReceiveSurveyHelper {
    private const val MODE_INACTIVE = 0
    private const val MODE_ACTIVE = 1
    private const val MODE_OVERRIDE = 2

    fun activateSurvey() {
        if (!isActive()) {
            Prefs.readingListReceiveSurveyMode = MODE_ACTIVE
        }
    }

    fun maybeShowSurvey(activity: Activity) {
        if (shouldShowSurvey(activity)) {
            showSurveyDialog(activity)
        }
    }

    fun shouldShowSurvey(activity: Activity): Boolean {
        return !activity.isDestroyed && !Prefs.readingListReceiveSurveyDialogShown &&
                (Prefs.readingListReceiveSurveyMode == MODE_OVERRIDE || (isActive() && fallsWithinDateRange()))
    }

    private fun showSurveyDialog(activity: Activity) {
        Prefs.readingListReceiveSurveyDialogShown = true

        val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.reading_list_share_survey_title))
                .setMessage(StringUtil.fromHtml(activity.getString(R.string.reading_list_share_survey_body) +
                        "<br/><br/><small><a href=\"${getLanguageSpecificPrivacyPolicyUrl()}\">" +
                        activity.getString(R.string.privacy_policy_description) + "</a></small>"))
                .setPositiveButton(R.string.talk_snackbar_survey_action_text) { _, _ -> takeUserToSurvey(activity) }
                .setNegativeButton(R.string.reading_list_prompt_turned_sync_on_dialog_no_thanks, null)
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { url ->
            CustomTabsUtil.openInCustomTab(activity, url)
        }
        ReadingListsAnalyticsHelper.logSurveyShown(activity)
    }

    private fun isActive(): Boolean {
        return Prefs.readingListReceiveSurveyMode != MODE_INACTIVE
    }

    private fun fallsWithinDateRange(): Boolean {
        return LocalDate.now() < LocalDate.of(2023, Month.APRIL, 17)
    }

    private fun takeUserToSurvey(context: Context) {
        CustomTabsUtil.openInCustomTab(context, getLanguageSpecificUrl())
    }

    private fun getLanguageSpecificUrl(): String {
        return when (WikipediaApp.instance.languageState.appLanguageCode) {
            "ar" -> "https://docs.google.com/forms/d/e/1FAIpQLSeKCRBtnF4V1Gwv2aRsJi8GppfofbiECU6XseZbVRbYijynfg/viewform?usp=sf_link"
            "bn" -> "https://docs.google.com/forms/d/e/1FAIpQLSeY25GeA8dFOKlVCNpHc5zTUIYUeB3W6fntTitTIQRjl7BCQw/viewform?usp=sf_link"
            "fr" -> "https://docs.google.com/forms/d/e/1FAIpQLSe_EXLDJxk-9y0ux-c9LERNou7CqhzoSZfL952PKH8bqCGMpA/viewform?usp=sf_link"
            "de" -> "https://docs.google.com/forms/d/e/1FAIpQLSfS2-gQJtCUnFMJl-C0BdrWNxpb-PeXjoDeCR4z80gSCoA-RA/viewform?usp=sf_link"
            "hi" -> "https://docs.google.com/forms/d/e/1FAIpQLSdnjiMH4L9eIpwuk3JLdsjKirvQ5GvLwp_8aaLKiESf-zhtHA/viewform?usp=sf_link"
            "pt" -> "https://docs.google.com/forms/d/e/1FAIpQLSfbRhbf-cqmZC-vn1S_OTdsJ0zpiVW7vfFpWQgZtzQbU0dZEw/viewform?usp=sf_link"
            "es" -> "https://docs.google.com/forms/d/e/1FAIpQLSelTK2ZeuEOk2T9P-E5OeKZoE9VvmCXLx9v3lc-A-onWXSsog/viewform?usp=sf_link"
            "ur" -> "https://docs.google.com/forms/d/e/1FAIpQLSdPcGIn049-8g-JgxJ8lFRa8UGg4xcWdL6Na18GuDCUD8iUXA/viewform?usp=sf_link"
            else -> "https://docs.google.com/forms/d/e/1FAIpQLSf7W1Hs20HcP-Ho4T_Rlr8hdpT4oKxYQJD3rdE5RCINl5l6RQ/viewform?usp=sf_link"
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
