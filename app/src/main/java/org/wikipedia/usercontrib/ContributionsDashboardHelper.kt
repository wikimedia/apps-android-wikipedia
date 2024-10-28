package org.wikipedia.usercontrib

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

class ContributionsDashboardHelper {

    companion object {

        private val enabledCountries = listOf(
            "FR", "NL"
        )

        private val enabledLanguages = listOf(
            "fr", "nl", "en"
        )

        val contributionsDashboardEnabled get() = ReleaseUtil.isPreBetaRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2024, 12, 20))

        fun showDonationCompletedDialog(context: Context) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon)
                .setTitle(R.string.contributions_dashboard_donation_dialog_title)
                .setMessage(R.string.contributions_dashboard_donation_dialog_message)
                .setIcon(R.drawable.ic_baseline_volunteer_activism_24) // TODO: change to use outline
                .setPositiveButton(R.string.contributions_dashboard_donation_dialog_ok) { _, _ ->
                    // TODO: Go to donor history
                }
                .setNegativeButton(R.string.contributions_dashboard_donation_dialog_cancel) { _, _ ->
                    // TODO: Update preference
                }
                .show()
        }

        fun showEntryDialog(context: Context) {
            MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon)
                .setTitle(R.string.contributions_dashboard_entry_dialog_title)
                .setMessage(R.string.contributions_dashboard_entry_dialog_message)
                .setIcon(R.drawable.ic_baseline_volunteer_activism_24) // TODO: change to use outline
                .setPositiveButton(R.string.contributions_dashboard_entry_dialog_ok) { _, _ ->
                    // TODO: Go to donor history
                }
                .setNegativeButton(R.string.contributions_dashboard_entry_dialog_cancel) { _, _ ->
                    // TODO: Update preference
                }
                .show()
        }
    }
}
