package org.wikipedia.donate.donationreminder

import android.app.Activity
import android.widget.ScrollView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import kotlinx.serialization.Serializable
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.databinding.DialogFeedbackOptionsBinding
import org.wikipedia.donate.DonateUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.LocalDate

object DonationReminderHelper {
    const val CAMPAIGN_ID = "appmenu_reminder"
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
                config.articleVisit % config.articleFrequency == 0 &&
                !config.initialPromptActive) {
                // When reaching the article frequency, activate the reminder and reset the count and visits
                Prefs.donationReminderConfig = config.copy(
                    finalPromptActive = true,
                    finalPromptCount = 0,
                    articleVisit = 0
                )
            }
        }
    }

    fun maybeShowSurveyDialog(activity: Activity) {
        if (!isEnabled) return

        val config = Prefs.donationReminderConfig
        if (config.isSurveyShown) return

        // when user sets up the reminder
        val hasSetupReminder = config.donateAmount > 0 && config.articleFrequency > 0
        if (hasSetupReminder) {
            val userGroup = getUserGroup()
            when (userGroup) {
                "A" -> {
                    // Group A: Show survey on next article visit after setting up reminder
                    showFeedbackOptionsDialog(activity)
                }
                "B" -> {
                    // Group B: Show survey on the next article visit after seeing reminder impressions
                    if (config.finalPromptCount >= 1) {
                        showFeedbackOptionsDialog(activity)
                    }
                }
            }
            return
        }

        // User has not taken any action on the initial prompt
        // Show survey on next article visit if this continues for continuous 5 times
        if (config.initialPromptCount >= MAX_INITIAL_REMINDER_PROMPTS) {
            showFeedbackOptionsDialog(activity)
            return
        }
    }

    private fun getUserGroup(): String {
        return if (Prefs.appInstallId.hashCode() % 2 == 0) "A" else "B"
    }

    // @TODO: MARK_INSTRUMENTATION: update if PM decides to use different activeInterface for difference scenarios
    private fun showFeedbackOptionsDialog(activity: Activity) {
        val binding = DialogFeedbackOptionsBinding.inflate(activity.layoutInflater)
        binding.titleText.text = activity.getString(R.string.donation_reminders_survey_dialog_title)
        binding.messageText.text = activity.getString(R.string.donation_reminders_survey_dialog_message)
        binding.feedbackInputContainer.isVisible = true

        val dialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.cancelButton.setOnClickListener {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_feedback",
                action = "feedback_close_click"
            )
            dialog.dismiss()
        }
        binding.submitButton.setOnClickListener {
            val selectedOption = getSelectedOption(binding)
            val feedbackText = binding.feedbackInput.text.toString()
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_feedback",
                action = "feedback_submit_click",
                feedbackSelect = selectedOption,
                feedbackText = feedbackText
            )
            dialog.dismiss()
        }

        binding.feedbackInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.dialogContainer.postDelayed({
                    if (!activity.isDestroyed) {
                        binding.dialogContainer.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }, 200)
            }
        }
        DonorExperienceEvent.logDonationReminderAction(activeInterface = "reminder_feedback", action = "impression")
        dialog.show()
        Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(isSurveyShown = true)
    }

    private fun getSelectedOption(binding: DialogFeedbackOptionsBinding): Int? {
        val selectedId = binding.feedbackRadioGroup.checkedRadioButtonId
        return when (selectedId) {
            R.id.optionSatisfied -> 1
            R.id.optionNeutral -> 2
            R.id.optionUnsatisfied -> 3
            else -> null
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
