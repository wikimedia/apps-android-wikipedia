package org.wikipedia.donate.donationreminder

import android.app.Activity
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.donate.DonateUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {
    const val CAMPAIGN_ID = "appmenu_reminder"
    const val MAX_REMINDER_PROMPTS = 2
    private val validReadCountOnSeconds = if (ReleaseUtil.isDevRelease) 1 else 15

    private val isTestGroupUser = DonationReminderAbTest().isTestGroupUser()
    private val enabledCountries = listOf(
        "GB", "AU", "CA"
    )
    private val isInEligibleCountry get() = ReleaseUtil.isDevRelease || enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty())

    val defaultReadFrequencyOptions = listOf(5, 10, 15, 25, 50)

    val isEnabled
        get() = ReleaseUtil.isDevRelease || isInEligibleCountry &&
                        LocalDate.now() <= LocalDate.of(2026, 3, 15) && isTestGroupUser

    val hasActiveReminder get() = Prefs.donationReminderConfig.isEnabled && Prefs.donationReminderConfig.finalPromptActive && isInEligibleCountry

    var shouldShowSettingSnackbar = false

    fun thankYouMessageForSettings(): String {
        val context = WikipediaApp.instance
        val donationAmount =
            DonateUtil.currencyFormat.format(Prefs.donationReminderConfig.donateAmount)
        val readFrequency = Prefs.donationReminderConfig.articleFrequency
        val articleNumber = context.resources.getQuantityString(R.plurals.donation_reminders_text_articles,
            readFrequency, readFrequency)
        val message = context.getString(R.string.donation_reminders_snacbkbar_confirmation_label, donationAmount, articleNumber)
        return message
    }

    fun maybeShowSettingSnackbar(activity: Activity) {
        if (shouldShowSettingSnackbar) {
            FeedbackUtil.makeNavigationAwareSnackbar(activity, thankYouMessageForSettings()).show()
            shouldShowSettingSnackbar = false
        }
    }

    fun increaseArticleVisitCount(timeSpentSec: Int) {
        var config = Prefs.donationReminderConfig
        if (timeSpentSec >= validReadCountOnSeconds && !config.finalPromptActive && config.setupTimestamp != 0L) {
            Prefs.donationReminderConfig = config.copy(
                articleVisit = config.articleVisit + 1
            )
            activateDonationReminder()
        }
        config = Prefs.donationReminderConfig
        if (config.finalPromptActive && config.finalPromptCount == MAX_REMINDER_PROMPTS) {
            // When user reaches the maximum reminder prompts, then turn off the final prompt
            Prefs.donationReminderConfig = config.copy(
                finalPromptActive = false
            )
        }
    }

    fun donationReminderDismissed() {
        val config = Prefs.donationReminderConfig
        Prefs.donationReminderConfig = config.copy(finalPromptActive = false)
    }

    fun maybeShowDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        return Prefs.donationReminderConfig.let { config ->
            val daysOfLastSeen = (LocalDate.now().toEpochDay() - config.promptLastSeen)
            if (!config.isEnabled || config.setupTimestamp == 0L || !config.finalPromptActive ||
                config.finalPromptCount > MAX_REMINDER_PROMPTS ||
                daysOfLastSeen <= 0
            ) {
                return@let false
            }

            if (update) {
                val finalPromptCount = config.finalPromptCount + 1
                Prefs.donationReminderConfig = config.copy(
                    finalPromptCount = finalPromptCount,
                    promptLastSeen = LocalDate.now().toEpochDay()
                )
            }
            return true
        }
    }

    private fun activateDonationReminder() {
        Prefs.donationReminderConfig.let { config ->
            if (config.articleVisit > 0 && config.articleFrequency > 0 &&
                config.articleVisit % config.articleFrequency == 0) {
                // When reaching the article frequency, activate the reminder and reset the count and visits
                Prefs.donationReminderConfig = config.copy(
                    finalPromptActive = true,
                    finalPromptCount = 0,
                    articleVisit = 0,
                    goalReachedCount = config.goalReachedCount + 1
                )
            }
        }
    }
}

@Serializable
data class DonationReminderConfig(
    val isEnabled: Boolean = false,
    val finalPromptCount: Int = 0,
    val finalPromptActive: Boolean = false,
    val promptLastSeen: Long = 0,
    val setupTimestamp: Long = 0,
    val articleVisit: Int = 0,
    val articleFrequency: Int = 0,
    val donateAmount: Float = 0f,
    val goalReachedCount: Int = 0
)
