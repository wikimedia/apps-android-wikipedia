package org.wikipedia.usercontrib

import android.content.Context
import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.ContributionsDashboardEvent
import org.wikipedia.donate.DonorHistoryActivity
import org.wikipedia.donate.DonorStatus
import org.wikipedia.settings.SettingsActivity
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
                "fr" to "https://docs.google.com/forms/d/1EfPNslpWWQd1WQoA3IkFRKhWy02BTaBgTer1uCKiIHU/edit",
                "nl" to "https://docs.google.com/forms/d/15GXIEfQTujFtXNU5NqfDyr9lxxET6fP0hk_p_Xz-NOk/edit",
                "en" to "https://docs.google.com/forms/d/1wIJWp75MMyp2e51kSaPH9ctByUzbyhazEOaJTxQhKqs/edit"
            )
            return surveyUrls[WikipediaApp.instance.languageState.appLanguageCode].orEmpty()
        }

        var shouldShowDonorHistorySnackbar = false

        var shouldShowThankYouDialog = false

        var showSurveyDialogUI = false

        val contributionsDashboardEnabled get() = ReleaseUtil.isPreBetaRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2024, 12, 20))

        fun showSurveyDialog(context: Context, onNegativeButtonClick: () -> Unit) {
            ContributionsDashboardEvent.logAction("impression", "contrib_survey")
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_survey_dialog_title)
                .setMessage(R.string.contributions_dashboard_survey_dialog_message)
                .setIcon(R.drawable.ic_feedback)
                .setCancelable(false)
                .setPositiveButton(R.string.contributions_dashboard_survey_dialog_ok) { _, _ ->
                    ContributionsDashboardEvent.logAction("enter_click", "contrib_survey")
                    // this should be called on button click due to logic in onResume
                    setEitherShowDialogOrSnackBar()
                    UriUtil.visitInExternalBrowser(
                        context,
                        Uri.parse(getSurveyDialogUrl())
                    )
                }
                .setNegativeButton(R.string.contributions_dashboard_survey_dialog_cancel) { _, _ ->
                    ContributionsDashboardEvent.logAction("cancel_click", "contrib_survey")
                    // this should be called on button click due to logic in onResume
                    setEitherShowDialogOrSnackBar()
                    onNegativeButtonClick()
                }
                .show()
        }

        fun showThankYouDialog(context: Context) {
            ContributionsDashboardEvent.logAction("impression", "contrib_icon_offer")
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_donor_icon_dialog_title)
                .setMessage(R.string.contributions_dashboard_donor_icon_dialog_message)
                .setIcon(R.drawable.ic_heart_24)
                .setPositiveButton(R.string.contributions_dashboard_donor_icon_dialog_ok) { _, _ ->
                    ContributionsDashboardEvent.logAction("enter_click", "contrib_icon_offer")
                    context.startActivity(SettingsActivity.newIntent(context, showAppIconDialog = true))
                }
                .setNegativeButton(R.string.contributions_dashboard_donor_icon_dialog_cancel) { _, _ ->
                    ContributionsDashboardEvent.logAction("cancel_click", "contrib_icon_offer")
                }
                .show()
        }

        fun showDonationCompletedDialog(context: Context) {
            ContributionsDashboardEvent.logAction("impression", "contrib_donor_banner")
            val message = String.format(context.getString(R.string.contributions_dashboard_donation_dialog_message))
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_donation_dialog_title)
                .setMessage(message)
                .setIcon(R.drawable.outline_volunteer_activism_24)
                .setPositiveButton(R.string.contributions_dashboard_donation_dialog_ok) { _, _ ->
                    ContributionsDashboardEvent.logAction("contrib_enter_click", "contrib_donor_banner")
                    context.startActivity(DonorHistoryActivity.newIntent(context, completedDonation = true, goBackToContributeTab = true))
                }
                .setNegativeButton(R.string.contributions_dashboard_donation_dialog_cancel, { _, _ ->
                    ContributionsDashboardEvent.logAction("contrib_enter_click", "contrib_cancel_click")
                })
                .show()
        }

        fun showEntryDialog(context: Context) {
            ContributionsDashboardEvent.logAction("impression", "contrib_banner")
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon_Secondary)
                .setTitle(R.string.contributions_dashboard_entry_dialog_title)
                .setMessage(R.string.contributions_dashboard_entry_dialog_message)
                .setIcon(R.drawable.outline_volunteer_activism_24)
                .setPositiveButton(R.string.contributions_dashboard_entry_dialog_ok) { _, _ ->
                    ContributionsDashboardEvent.logAction("contrib_enter_click", "contrib_banner")
                    context.startActivity(DonorHistoryActivity.newIntent(context, goBackToContributeTab = true))
                }
                .setNegativeButton(R.string.contributions_dashboard_entry_dialog_cancel, { _, _ ->
                    ContributionsDashboardEvent.logAction("contrib_cancel_click", "contrib_banner")
                })
                .show()
        }

        private fun setEitherShowDialogOrSnackBar() {
            when (DonorStatus.donorStatus()) {
                DonorStatus.DONOR -> shouldShowThankYouDialog = true
                DonorStatus.NON_DONOR -> shouldShowDonorHistorySnackbar = true
                DonorStatus.UNKNOWN -> {}
            }
        }
    }
}
