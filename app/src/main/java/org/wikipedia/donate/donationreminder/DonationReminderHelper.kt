package org.wikipedia.donate.donationreminder

import android.app.Activity
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.databinding.DialogFeedbackOptionsBinding
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.ReleaseUtil
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DonationReminderHelper {
    private val enabledCountries = listOf(
        "IT"
    )

    private val enabledLanguages = listOf(
        "it", "en"
    )

    // TODO: update the end date when before release to production for 30-day experiment
    val isEnabled get() = ReleaseUtil.isDevRelease ||
            (enabledCountries.contains(GeoUtil.geoIPCountry.orEmpty()) &&
                    enabledLanguages.contains(WikipediaApp.instance.languageState.appLanguageCode) &&
                    LocalDate.now() <= LocalDate.of(2025, 12, 1) && !AccountUtil.isLoggedIn)

    val currencyAmountPresets = mapOf(
        "IT" to listOf(1f, 2f, 3f)
    )

    val defaultReadFrequencyOptions = listOf(5, 10, 15)

    fun getDonationReminderSubmittedFormDate(): String {
        val timeStamp = Prefs.donationReminderConfig.setupTimestamp
        val localDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timeStamp),
            ZoneId.systemDefault()
        )
        val formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
        return localDateTime.format(formatter)
    }

    fun maybeShowSurveyDialog(activity: Activity) {
        if (!isEnabled) return
        if (Prefs.donationReminderConfig.isSurveyShown) return

        // when user sets up the reminder
        val hasSetupReminder = Prefs.donationReminderConfig.donateAmount > 0 && Prefs.donationReminderConfig.articleFrequency > 0
        if (hasSetupReminder) {
            val userGroup = getUserGroup()
            when (userGroup) {
                "A" -> {
                    // Group A: Show survey on next article visit after setting up reminder
                    if (Prefs.donationReminderConfig.isReadyToShowSurvey) {
                        showFeedbackOptionsDialog(activity, Constants.InvokeSource.PAGE_ACTIVITY)
                    }
                }
                "B" -> {
                    // Group B: Show survey on the next article visit after seeing reminder impressions two times
                    if (Prefs.donationReminderConfig.finalPromptCount == -1 && Prefs.donationReminderConfig.isReadyToShowSurvey) {
                        showFeedbackOptionsDialog(activity, Constants.InvokeSource.PAGE_ACTIVITY)
                    }
                }
            }
            return
        }

        // User has not taken any action on the initial prompt
        // Show survey on next article visit if this continues for continuous 5 times
        if (Prefs.donationReminderConfig.initialPromptCount == -1 && Prefs.donationReminderConfig.isReadyToShowSurvey) {
            showFeedbackOptionsDialog(activity, Constants.InvokeSource.PAGE_ACTIVITY)
            return
        }
    }

    private fun getUserGroup(): String {
        return if (Prefs.appInstallId.hashCode() % 2 == 0) "A" else "B"
    }

    private fun showFeedbackOptionsDialog(activity: Activity, invokeSource: Constants.InvokeSource) {
        var dialog: AlertDialog? = null
        val binding = DialogFeedbackOptionsBinding.inflate(activity.layoutInflater)
        binding.titleText.text = activity.getString(R.string.donation_reminders_survey_dialog_title)
        binding.messageText.text = activity.getString(R.string.donation_reminders_survey_dialog_message)
        binding.feedbackInputContainer.isVisible = true
        binding.feedbackInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.dialogContainer.postDelayed({
                    binding.dialogContainer.fullScroll(ScrollView.FOCUS_DOWN)
                }, 200)
            }
        }

        val clickListener = View.OnClickListener {
            val feedbackOption = (it as TextView).text.toString()
            dialog?.dismiss()
        }
        binding.optionSatisfied.setOnClickListener(clickListener)
        binding.optionNeutral.setOnClickListener(clickListener)
        binding.optionUnsatisfied.setOnClickListener(clickListener)
        binding.cancelButton.setOnClickListener { dialog?.dismiss() }
        binding.submitButton.setOnClickListener(clickListener)

        val dialogBuilder = MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_AdjustResize)
            .setCancelable(false)
            .setView(binding.root)
        dialog = dialogBuilder.show()
        Prefs.donationReminderConfig = Prefs.donationReminderConfig.copy(isSurveyShown = true)
    }
}

@Serializable
data class DonationReminderConfig(
    val isEnabled: Boolean = false,
    val initialPromptCount: Int = 0,
    val initialPromptDismissed: Boolean = false,
    val finalPromptCount: Int = 0,
    val finalPromptDismissed: Boolean = false,
    val promptLastSeen: Long = 0,
    val setupTimestamp: Long = 0,
    val articleVisit: Int = 0,
    val isSurveyShown: Boolean = false,
    val articleFrequency: Int = 0,
    val donateAmount: Float = 0f
)
