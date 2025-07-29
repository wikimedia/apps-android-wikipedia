package org.wikipedia.donate.donationreminder

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {

    const val MAX_INITIAL_REMINDER_PROMPTS = 5
    const val MAX_REMINDER_PROMPTS = 2

    private val enabledCountries = listOf(
        "IT"
    )

    private val enabledLanguages = listOf(
        "it", "en"
    )

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled get() = ReleaseUtil.isDevRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    enabledLanguages.contains(WikipediaApp.Companion.instance.languageState.appLanguageCode) &&
                    LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)

    val hasActiveReminder get() = maybeShowInitialDonationReminder(false) || maybeShowDonationReminder(false)
    val shouldShowInitialPrompt get() = true // TODO: use the real logic from donation reminder settings

    fun maybeShowInitialDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        val daysOfLastSeen = (LocalDate.now().toEpochDay() - Prefs.donationReminderInitialPromptLastSeen)
        if (Prefs.donationReminderInitialPromptCount == -1 ||
            Prefs.donationReminderInitialPromptCount >= MAX_INITIAL_REMINDER_PROMPTS ||
            (daysOfLastSeen <= 0 && Prefs.donationReminderInitialPromptCount > 0)) {
            return false
        }
        if (update) {
            Prefs.donationReminderInitialPromptCount += 1
            Prefs.donationReminderInitialPromptLastSeen = LocalDate.now().toEpochDay()
        }
        if (Prefs.donationReminderInitialPromptCount >= MAX_INITIAL_REMINDER_PROMPTS) {
            // Set it to -1 to mark it as done.
            Prefs.donationReminderInitialPromptCount = -1
        }
        return true
    }

    // TODO: connect the logic with donation reminder settings (e.g. article numbers, donation amount, etc.)
    fun maybeShowDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        return false // TODO: temporary disable the donation reminder prompt
        val daysOfLastSeen = (LocalDate.now().toEpochDay() - Prefs.donationReminderPromptLastSeen)
        if (Prefs.donationReminderPromptCount == -1 ||
            Prefs.donationReminderPromptCount >= MAX_REMINDER_PROMPTS ||
            (daysOfLastSeen <= 0 && Prefs.donationReminderPromptCount > 0)) {
            return false
        }
        if (update) {
            Prefs.donationReminderPromptCount += 1
            Prefs.donationReminderPromptLastSeen = LocalDate.now().toEpochDay()
        }
        if (Prefs.donationReminderPromptCount >= MAX_REMINDER_PROMPTS) {
            // Set it to -1 to mark it as done.
            Prefs.donationReminderPromptCount = -1
        }
        return true
    }
}
