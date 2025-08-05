package org.wikipedia.donate.donationreminder

import android.app.Activity
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.donate.DonateUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {

    const val MAX_INITIAL_REMINDER_PROMPTS = 5
    const val MAX_REMINDER_PROMPTS = 2
    private val validReadCountOnSeconds = if (ReleaseUtil.isDevRelease) 1 else 15
    private val enabledCountries = listOf(
        "IT"
    )

    private val enabledLanguages = listOf(
        "it", "en"
    )

    val currencyAmountPresets = mapOf(
        "IT" to listOf(1f, 2f, 3f)
    )

    val defaultReadFrequencyOptions = listOf(5, 10, 15)

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled
        get() = ReleaseUtil.isDevRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.Companion.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)

    val hasActiveReminder get() = Prefs.donationReminderConfig.initialPromptActive ||
            (Prefs.donationReminderConfig.isEnabled && Prefs.donationReminderConfig.finalPromptActive)

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
        val config = Prefs.donationReminderConfig
        if (timeSpentSec >= validReadCountOnSeconds && !config.finalPromptActive && config.setupTimestamp != 0L) {
            Prefs.donationReminderConfig = config.copy(
                articleVisit = config.articleVisit + 1
            )
            resetDonationReminder()
        }
        if (config.finalPromptActive && config.finalPromptCount == MAX_REMINDER_PROMPTS) {
            // When user reaches the maximum reminder prompts, then turn off the final prompt
            Prefs.donationReminderConfig = config.copy(
                finalPromptActive = false
            )
        }
    }

    fun donationReminderDismissed(isInitialPrompt: Boolean) {
        val config = Prefs.donationReminderConfig
        Prefs.donationReminderConfig = if (isInitialPrompt) {
            config.copy(initialPromptActive = false)
        } else {
            config.copy(finalPromptActive = false)
        }
    }

    fun maybeShowInitialDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        return Prefs.donationReminderConfig.let { config ->
            val daysOfLastSeen = (LocalDate.now().toEpochDay() - config.promptLastSeen)
            if (config.setupTimestamp > 0L || !config.initialPromptActive ||
                config.initialPromptCount >= MAX_INITIAL_REMINDER_PROMPTS ||
                daysOfLastSeen <= 0
            ) {
                return@let false
            }
            if (update) {
                Prefs.donationReminderConfig = config.copy(
                    initialPromptCount = config.initialPromptCount + 1,
                    promptLastSeen = LocalDate.now().toEpochDay()
                )
            }
            return true
        }
    }

    fun maybeShowDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        return Prefs.donationReminderConfig.let { config ->
            val daysOfLastSeen = (LocalDate.now().toEpochDay() - config.promptLastSeen)
            if (!config.isEnabled || config.setupTimestamp == 0L || !config.finalPromptActive ||
                config.finalPromptCount > (MAX_REMINDER_PROMPTS + 1) ||
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

    private fun resetDonationReminder() {
        Prefs.donationReminderConfig.let { config ->
            if (config.articleVisit > 0 && config.articleFrequency > 0 &&
                config.articleVisit % config.articleFrequency == 0 &&
                !config.initialPromptActive) {
                // When reaching the article frequency, reset the configuration
                Prefs.donationReminderConfig = config.copy(
                    finalPromptActive = true,
                    finalPromptCount = 0,
                    articleVisit = 0
                )
            }
        }
    }
}

@Serializable
data class DonationReminderConfig(
    val isEnabled: Boolean = false,
    val initialPromptCount: Int = 0,
    val initialPromptActive: Boolean = true,
    val finalPromptCount: Int = 0,
    val finalPromptActive: Boolean = false,
    val promptLastSeen: Long = 0,
    val setupTimestamp: Long = 0,
    val articleVisit: Int = 0,
    val isSurveyShown: Boolean = false,
    val articleFrequency: Int = 0,
    val donateAmount: Float = 0f
)
