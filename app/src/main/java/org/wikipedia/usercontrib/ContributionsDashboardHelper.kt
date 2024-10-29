package org.wikipedia.usercontrib

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
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

        fun maybeShowContributionsDashboardSurveyDialog(context: Context, delay: Long = 0) {
            if (!Prefs.contributionsDashboardSurveyDialogShown || Prefs.hasDonorHistorySaved) {
                Handler(Looper.getMainLooper()).postDelayed({
                    MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme_Icon)
                        .setTitle(R.string.contributions_dashboard_survey_dialog_title)
                        .setMessage(R.string.contributions_dashboard_survey_dialog_message)
                        .setIcon(R.drawable.ic_feedback)
                        .setCancelable(false)
                        .setPositiveButton(R.string.contributions_dashboard_survey_dialog_ok) { _, _ ->
                            UriUtil.visitInExternalBrowser(
                                context,
                                Uri.parse(context.getString(R.string.contributions_dashboard_survey_url))
                            )
                        }
                        .setNegativeButton(R.string.contributions_dashboard_survey_dialog_cancel, null)
                        .show()
                    Prefs.contributionsDashboardSurveyDialogShown = true
                }, delay)
            }
        }

        val contributionsDashboardEnabled get() = ReleaseUtil.isPreBetaRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2024, 12, 20))
    }
}
