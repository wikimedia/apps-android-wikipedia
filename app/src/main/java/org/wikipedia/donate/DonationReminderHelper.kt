package org.wikipedia.donate

import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
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
    val isEnabled
        get() = ReleaseUtil.isDevRelease ||
                (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                        enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                        LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)

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
        return true
    }

    // TODO: connect the logic with donation reminder settings (e.g. article numbers, donation amount, etc.)
    fun maybeShowDonationReminder(update: Boolean = false): Boolean {
        if (!isEnabled) return false
        val daysOfLastSeen = (LocalDate.now().toEpochDay() - Prefs.donationReminderPromptLastSeen)
        if (Prefs.donationReminderPromptCount == -1 ||
            Prefs.donationReminderPromptCount >= MAX_REMINDER_PROMPTS ||
            (daysOfLastSeen <= 0 && Prefs.donationReminderInitialPromptCount > 0)) {
            return false
        }
        if (update) {
            Prefs.donationReminderPromptCount += 1
            Prefs.donationReminderPromptLastSeen = LocalDate.now().toEpochDay()
        }
        return true
    }

    fun onDonationPromptImpressionSeen() {
        Prefs.donationReminderSurveySate = DonationReminderSurveyState.PROMPT_IMPRESSIONS_SEEN
    }

    fun onDonationReminderImpressionsSeen() {
        Prefs.donationReminderSurveySate =
            DonationReminderSurveyState.REMINDER_SET_GROUP_B_READY_FOR_SURVEY
    }

    // @TODO: call from donation reminder settings confirmation
    fun onDonationReminderSetUp() {
        Prefs.donationReminderSurveySate = DonationRemindersSurveyGroupAssignment().getUserGroup()
    }

    // @TODO: call when user clicks "No Thanks" from the prompt card
    fun onDonationReminderDeclined() {
        Prefs.donationReminderSurveySate = DonationReminderSurveyState.NO_REMINDER_READY_FOR_SURVEY
    }

    fun updateArticleVisit() {
        Prefs.donationReminderArticleVisit += 1
    }

    // donationRemindersReadFrequency and donationRemindersAmount != -1 means donation reminder is set
    // survey display points:
    // 1. next article after setting up reminder
    // 2. next article after seeing donation reminder impression
    // 3. next article for users who did not set up the reminder but show see their prompt impression
    // first we need to check if user has seen the donation reminder, if they have seen the
    fun maybeShowSurveyDialog() {
        val canShowSurvey = when (Prefs.donationReminderSurveySate) {
            DonationReminderSurveyState.REMINDER_SET_GROUP_A_READY_FOR_SURVEY,
            DonationReminderSurveyState.REMINDER_SET_GROUP_B_READY_FOR_SURVEY,
            DonationReminderSurveyState.NO_REMINDER_READY_FOR_SURVEY -> {
                println("orange --> maybeShowSurveyDialog article visit ${Prefs.donationReminderArticleVisit}")
                Prefs.donationReminderArticleVisit >= 2
            }
            else -> false
        }

        if (canShowSurvey) {
            Prefs.donationReminderArticleVisit = 1
            Prefs.donationReminderSurveySate = DonationReminderSurveyState.SURVEY_SHOWN
            println("orange --> survey")
        }
    }
}

enum class DonationReminderSurveyState {
    // Initial state - user hasn't seen prompts yet
    INITIAL,

    // User has seen prompt impressions but hasn't decided yet
    PROMPT_IMPRESSIONS_SEEN,

    // Flow A: User decided not to set up reminder
    NO_REMINDER_READY_FOR_SURVEY,

    // Flow B: User sets up reminder (either A or B for each user one time)
    REMINDER_SET_GROUP_A_READY_FOR_SURVEY, // 50% - show survey immediately on next article
    REMINDER_SET_GROUP_B_WAITING_FOR_IMPRESSIONS, // 50% - wait for reminder impressions to appear and show survey on next article
    REMINDER_SET_GROUP_B_READY_FOR_SURVEY, // After seeing reminder impressions

    // Final state
    SURVEY_SHOWN
}

class DonationRemindersSurveyGroupAssignment : ABTest("donation_reminders", 2) {
    fun getUserGroup(): DonationReminderSurveyState {
        return when (group) {
            GROUP_2 -> DonationReminderSurveyState.REMINDER_SET_GROUP_B_WAITING_FOR_IMPRESSIONS
            else -> DonationReminderSurveyState.REMINDER_SET_GROUP_A_READY_FOR_SURVEY
        }
    }
}
