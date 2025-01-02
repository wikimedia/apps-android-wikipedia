package org.wikipedia.usercontrib

import android.content.Context
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.UriUtil
import java.time.LocalDate

class ContributionsDashboardHelper {

    companion object {

        const val CAMPAIGN_ID = "contrib"

        private val enabledCountries = listOf(
            "FR", "NL"
        )

        private val enabledLanguages = listOf(
            "fr", "nl", "en"
        )

        private fun getSurveyDialogUrl(): String {
            val surveyUrls = mapOf(
                "fr" to "https://docs.google.com/forms/d/1EfPNslpWWQd1WQoA3IkFRKhWy02BTaBgTer1uCKiIHU/viewform",
                "nl" to "https://docs.google.com/forms/d/15GXIEfQTujFtXNU5NqfDyr9lxxET6fP0hk_p_Xz-NOk/viewform",
                "en" to "https://docs.google.com/forms/d/1wIJWp75MMyp2e51kSaPH9ctByUzbyhazEOaJTxQhKqs/viewform"
            )
            return surveyUrls[WikipediaApp.instance.languageState.appLanguageCode].orEmpty()
        }

        var showSurveyDialogUI = false

        val contributionsDashboardEnabled get() = ReleaseUtil.isPreBetaRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2024, 12, 20))

        fun showSurveyDialog(context: Context, onNegativeButtonClick: () -> Unit) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_survey_dialog_title)
                .setMessage(R.string.contributions_dashboard_survey_dialog_message)
                .setIcon(R.drawable.ic_feedback)
                .setCancelable(false)
                .setPositiveButton(R.string.contributions_dashboard_survey_dialog_ok) { _, _ ->
                    // this should be called on button click due to logic in onResume
                    UriUtil.visitInExternalBrowser(
                        context,
                        Uri.parse(getSurveyDialogUrl())
                    )
                }
                .setNegativeButton(R.string.contributions_dashboard_survey_dialog_cancel) { _, _ ->
                    // this should be called on button click due to logic in onResume
                    onNegativeButtonClick()
                }
                .show()
        }

        fun showDonationCompletedDialog(context: Context) {
            val message = String.format(context.getString(R.string.contributions_dashboard_donation_dialog_message))
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_donation_dialog_title)
                .setMessage(message)
                .setIcon(R.drawable.outline_volunteer_activism_24)
                .setPositiveButton(R.string.contributions_dashboard_donation_dialog_ok) { _, _ -> }
                .setNegativeButton(R.string.contributions_dashboard_donation_dialog_cancel, { _, _ -> })
                .show()
        }

        fun showEntryDialog(context: Context) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_entry_dialog_title)
                .setMessage(R.string.contributions_dashboard_entry_dialog_message)
                .setIcon(R.drawable.outline_volunteer_activism_24)
                .setPositiveButton(R.string.contributions_dashboard_entry_dialog_ok) { _, _ -> }
                .setNegativeButton(R.string.contributions_dashboard_entry_dialog_cancel, { _, _ -> })
                .show()
        }
    }
}
