package org.wikipedia.donate.donationreminder

import android.app.Activity
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.donate.DonateUtil
import org.wikipedia.donate.donationreminder.DonationReminderHelper.MAX_REMINDER_PROMPTS
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {
    const val MAX_REMINDER_PROMPTS = 2
    private val validReadCountOnSeconds = if (ReleaseUtil.isDevRelease) 1 else 5

    private val isTestGroupUser = DonationReminderAbTest().isTestGroupUser()
    private val enabledCountries = listOf(
        "GB", "AU", "CA"
    )
    val isInEligibleCountry get() = ReleaseUtil.isDevRelease || enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty())

    val defaultReadFrequencyOptions = listOf(5, 10, 15, 25, 50)

    val isEnabled
        get() = ReleaseUtil.isDevRelease || isInEligibleCountry &&
                LocalDate.now() <= LocalDate.of(2026, 3, 15) && isTestGroupUser

    val hasActiveReminder get() = Prefs.donationReminderConfig.userEnabled && Prefs.donationReminderConfig.isReminderReady && isInEligibleCountry

    val campaignId = "appmenu_" + (if (isTestGroupUser) "reminderB" else "reminderA")

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
            FeedbackUtil.showMessage(activity, thankYouMessageForSettings())
            shouldShowSettingSnackbar = false
        }
    }

    fun increaseArticleVisitCount(timeSpentSec: Int) {
        if (!isEnabled || timeSpentSec < validReadCountOnSeconds) return

        val config = Prefs.donationReminderConfig
        if (!config.isSetup) return

        val newArticleCount = config.articleVisit + 1
        if (newArticleCount >= config.articleFrequency) {
            Prefs.donationReminderConfig = config.copy(
                articleVisit = 0,
                isReminderReady = true,
                goalReachedCount = config.goalReachedCount + 1,
                continuousArticleVisit = config.continuousArticleVisit + 1
            )
        } else {
            Prefs.donationReminderConfig = config.copy(
                articleVisit = newArticleCount,
                continuousArticleVisit = config.continuousArticleVisit + 1
            )
        }
    }

    fun shouldShowReminderNow(): Boolean {
        if (!isEnabled) return false
        return Prefs.donationReminderConfig.shouldShowNow()
    }

    fun recordReminderShown() {
        val config = Prefs.donationReminderConfig
        val newCount = config.timesReminderShown + 1

        Prefs.donationReminderConfig = config.copy(
            timesReminderShown = if (newCount != MAX_REMINDER_PROMPTS) newCount else 0,
            promptLastSeen = LocalDate.now().toEpochDay(),
            // Deactivate reminder if we've shown it max times
            isReminderReady = newCount < MAX_REMINDER_PROMPTS,
            continuousArticleVisit = if (newCount != MAX_REMINDER_PROMPTS) config.continuousArticleVisit else 0
        )
    }

    fun dismissReminder() {
        val config = Prefs.donationReminderConfig
        Prefs.donationReminderConfig = config.copy(
            isReminderReady = false
        )
    }
}

@Serializable
data class DonationReminderConfig(
    val userEnabled: Boolean = false,
    val promptLastSeen: Long = 0,
    val setupTimestamp: Long = 0,
    val articleVisit: Int = 0,
    val articleFrequency: Int = 0,
    val donateAmount: Float = 0f,
    val isReminderReady: Boolean = false,
    val timesReminderShown: Int = 0,
    val goalReachedCount: Int = 0,
    var continuousArticleVisit: Int = 0
) {
    val isSetup: Boolean get() = userEnabled && setupTimestamp != 0L && articleFrequency > 0

    fun shouldShowNow(): Boolean {
        if (!isSetup || !isReminderReady || timesReminderShown >= MAX_REMINDER_PROMPTS) return false

        val daysSinceLastShown = LocalDate.now().toEpochDay() - promptLastSeen
        return daysSinceLastShown > 0
    }
}
