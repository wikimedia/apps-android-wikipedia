package org.wikipedia.donate.donationreminder

import kotlinx.serialization.Serializable
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {

    const val MAX_INITIAL_REMINDER_PROMPTS = 5
    const val MAX_REMINDER_PROMPTS = 2
    const val VALID_ARTICLE_SPENT = 15
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

    val hasActiveReminder
        get() = maybeShowInitialDonationReminder(false) || maybeShowDonationReminder(
            false
        )

    fun increaseArticleVisitCount(timeSpentSec: Int) {
        if (timeSpentSec >= VALID_ARTICLE_SPENT) {
            Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(
                articleVisit = Prefs.donationReminderConfig.articleVisit + 1
            )
        }
    }

    fun donationReminderDismissed(isInitialPrompt: Boolean) {
        Prefs.donationReminderConfig = if (isInitialPrompt) {
            Prefs.donationReminderConfig.copy(initialPromptDismissed = true)
        } else {
            Prefs.donationReminderConfig.copy(finalPromptDismissed = true)
        }
    }

    fun maybeShowInitialDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        return Prefs.donationReminderConfig.let { config ->
            val daysOfLastSeen = (LocalDate.now().toEpochDay() - config.promptLastSeen)
            if (config.setupTimestamp > 0L || config.initialPromptDismissed ||
                config.initialPromptCount >= MAX_INITIAL_REMINDER_PROMPTS ||
                (daysOfLastSeen <= 0 && config.initialPromptCount > 0)
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
            if (config.articleVisit % config.articleFrequency == 0 && config.articleVisit > 0) {
                // When reaching the article frequency, reset the configuration
                Prefs.donationReminderConfig = config.copy(
                    finalPromptHold = false,
                    finalPromptDismissed = false,
                    finalPromptCount = 0
                )
            }
            val daysOfLastSeen = (LocalDate.now().toEpochDay() - config.promptLastSeen)
            if (config.setupTimestamp == 0L || config.finalPromptDismissed ||
                (config.finalPromptCount == MAX_REMINDER_PROMPTS && !config.finalPromptHold) || // final prompt is not held
                config.finalPromptCount >= MAX_REMINDER_PROMPTS ||
                (daysOfLastSeen <= 0 && config.finalPromptCount > 0)
            ) {
                return@let false
            }

            if (update) {
                Prefs.donationReminderConfig = config.copy(
                    finalPromptCount = config.finalPromptCount + 1,
                    promptLastSeen = LocalDate.now().toEpochDay()
                )
            }
            return true
        }
    }
}

@Serializable
data class DonationReminderConfig(
    val isEnabled: Boolean = false,
    val initialPromptCount: Int = 0,
    val initialPromptDismissed: Boolean = false,
    val finalPromptCount: Int = 0,
    val finalPromptHold: Boolean = false,
    val finalPromptDismissed: Boolean = false,
    val promptLastSeen: Long = 0,
    val setupTimestamp: Long = 0,
    val articleVisit: Int = 0,
    val isSurveyShown: Boolean = false,
    val articleFrequency: Int = 0,
    val donateAmount: Float = 0f
)
