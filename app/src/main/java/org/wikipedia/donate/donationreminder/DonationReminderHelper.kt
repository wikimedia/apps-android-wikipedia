package org.wikipedia.donate.donationreminder

import android.app.Activity
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest.Companion.GROUP_2
import org.wikipedia.analytics.ABTest.Companion.GROUP_3
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
        "NL"
    )
    private val isInDateRange get() = LocalDate.now() <= LocalDate.of(2026, 11, 9)
    private val isInWrapUpDateRange get() = LocalDate.now() <= LocalDate.of(2026, 11, 15) &&
            LocalDate.now() >= LocalDate.of(2026, 11, 10)
    val isInEligibleCountry get() = ReleaseUtil.isDevRelease || enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty())
    val defaultReadFrequencyOptions = listOf(5, 10, 20)
    val presetsToRemoveFromConfig = listOf(2f, 10f, 15f) // V3 only.
    val defaultDonateAmountOptions = listOf(1f, 3f, 5f) // V3 only.

    val isEnabled
        get() = (ReleaseUtil.isDevRelease || isInEligibleCountry && isInDateRange) && isTestGroupUser
    val isWrapUpEnabled: Boolean
        get() {
            val config = Prefs.donationReminderConfig
            val group = DonationReminderAbTest().group

            val isWrapUpWindow = (Prefs.donationReminderDevWrapUp && config.wrapUpEnabled) ||
                    (isInEligibleCountry && isInWrapUpDateRange)
            val isEligibleGroup = (group == GROUP_2 && config.wrapUpEnabled) ||
                    (group == GROUP_3 && config.wrapUpEnabled && config.userEnabled)

            return isWrapUpWindow && isEligibleGroup
        }

    val hasActiveReminder get() = Prefs.donationReminderConfig.userEnabled && Prefs.donationReminderConfig.isReminderReady && isInEligibleCountry

    var shouldShowSettingSnackbar = false

    fun getCampaignId(campaignIdOriginal: String = "appmenu"): String {
        return if (isInEligibleCountry && (isInDateRange || isInWrapUpDateRange)) {
            campaignIdOriginal + when (DonationReminderAbTest().group) {
                GROUP_3 -> "_reminderC"
                GROUP_2 -> "_reminderB"
                else -> "_reminderA"
            }
        } else {
            campaignIdOriginal
        }
    }

    fun updateDonationPresets(presets: MutableSet<Float>?) {
        if (isEnabled) {
            presets?.removeAll(presetsToRemoveFromConfig.toSet())
            presets?.addAll(defaultDonateAmountOptions)
        }
    }

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
            // Activate reminder, reset counters and increment goal reached
            Prefs.donationReminderConfig = config.copy(
                articleVisit = 0,
                isReminderReady = true,
                timesReminderShown = 0,
                goalReachedCount = config.goalReachedCount + 1
            )
        } else {
            // Just increment articleVisit counter
            Prefs.donationReminderConfig = config.copy(
                articleVisit = newArticleCount
            )
        }
    }

    fun shouldShowReminderNow(): Boolean {
        if (!isEnabled) return false

        val config = Prefs.donationReminderConfig
        val shouldShowNow = config.shouldShowNow()
        if (shouldShowNow) {
            val newCount = config.timesReminderShown + 1
            Prefs.donationReminderConfig = config.copy(
                timesReminderShown = newCount,
                promptLastSeen = LocalDate.now().toEpochDay(),
                // Deactivate reminder if we've shown it max times
                isReminderReady = newCount < MAX_REMINDER_PROMPTS
            )
        }
        return shouldShowNow
    }

    fun dismissReminder() {
        val config = Prefs.donationReminderConfig
        if (isWrapUpEnabled) {
            Prefs.donationReminderConfig = config.copy(
                wrapUpEnabled = false
            )
            Prefs.donationReminderDevWrapUp = false
        } else {
            Prefs.donationReminderConfig = config.copy(
                isReminderReady = false
            )
        }
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
    val wrapUpEnabled: Boolean = false
) {
    val isSetup: Boolean get() = userEnabled && setupTimestamp != 0L && articleFrequency > 0

    fun shouldShowNow(): Boolean {
        if (!isSetup || !isReminderReady || timesReminderShown >= MAX_REMINDER_PROMPTS) return false

        val daysSinceLastShown = LocalDate.now().toEpochDay() - promptLastSeen
        return daysSinceLastShown > 0
    }
}
