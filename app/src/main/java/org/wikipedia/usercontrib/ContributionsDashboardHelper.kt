package org.wikipedia.usercontrib

import android.content.Context
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.donate.DonorHistoryActivity
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.UriUtil
import java.time.LocalDate

class ContributionsDashboardHelper {

    companion object {

        private val enabledCountries = listOf(
            "FR", "NL"
        )

        private val enabledLanguages = listOf(
            "fr", "nl", "en"
        )

        private fun getSurveyDialogUrl(): String {
            val surveyUrls = mapOf(
                "fr" to "https://docs.google.com/forms/d/1EfPNslpWWQd1WQoA3IkFRKhWy02BTaBgTer1uCKiIHU/edit",
                "nl" to "https://docs.google.com/forms/d/15GXIEfQTujFtXNU5NqfDyr9lxxET6fP0hk_p_Xz-NOk/edit",
                "en" to "https://docs.google.com/forms/d/1wIJWp75MMyp2e51kSaPH9ctByUzbyhazEOaJTxQhKqs/edit"
            )
            return surveyUrls[WikipediaApp.instance.languageState.appLanguageCode].orEmpty()
        }

        val contributionsDashboardEnabled get() = ReleaseUtil.isPreBetaRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2024, 12, 20))

        fun showSurveyDialog(context: Context) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon)
                .setTitle(R.string.contributions_dashboard_survey_dialog_title)
                .setMessage(R.string.contributions_dashboard_survey_dialog_message)
                .setIcon(R.drawable.ic_feedback)
                .setCancelable(false)
                .setPositiveButton(R.string.contributions_dashboard_survey_dialog_ok) { _, _ ->
                    UriUtil.visitInExternalBrowser(
                        context,
                        Uri.parse(getSurveyDialogUrl())
                    )
                }
                .setNegativeButton(R.string.contributions_dashboard_survey_dialog_cancel, null)
                .show()
        }

        fun showDonationCompletedDialog(context: Context) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon)
                .setTitle(R.string.contributions_dashboard_donation_dialog_title)
                .setMessage(R.string.contributions_dashboard_donation_dialog_message)
                .setIcon(R.drawable.outline_volunteer_activism_24)
                .setPositiveButton(R.string.contributions_dashboard_donation_dialog_ok) { _, _ ->
                    context.startActivity(DonorHistoryActivity.newIntent(context, completedDonation = true, goBackToContributeTab = true))
                }
                .setNegativeButton(R.string.contributions_dashboard_donation_dialog_cancel, null)
                .show()
        }

        fun showEntryDialog(context: Context) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon)
                .setTitle(R.string.contributions_dashboard_entry_dialog_title)
                .setMessage(R.string.contributions_dashboard_entry_dialog_message)
                .setIcon(R.drawable.outline_volunteer_activism_24)
                .setPositiveButton(R.string.contributions_dashboard_entry_dialog_ok) { _, _ ->
                    context.startActivity(DonorHistoryActivity.newIntent(context, goBackToContributeTab = true))
                }
                .setNegativeButton(R.string.contributions_dashboard_entry_dialog_cancel, null)
                .show()
        }
    }
}
