package org.wikipedia.yearinreview

import android.app.Activity
import android.widget.ScrollView
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogFeedbackOptionsBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.FeedbackUtil

object YearInReviewSurvey {
    fun maybeShowYearInReviewFeedbackDialog(activity: Activity) {
        if (Prefs.yearInReviewSurveyState != YearInReviewSurveyState.SHOULD_SHOW) {
            return
        }
        val binding = DialogFeedbackOptionsBinding.inflate(activity.layoutInflater)
        binding.titleText.text = activity.getString(R.string.year_in_review_survey_title)
        binding.messageText.text = activity.getString(R.string.year_in_review_survey_subtitle)
        binding.feedbackInputContainer.isVisible = true
        binding.optionVerySatisfied.isVisible = true
        binding.optionVeryUnsatisfied.isVisible = true
        binding.feedbackInputContainer.hint =
            activity.getString(R.string.year_in_review_survey_placeholder_text)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        binding.submitButton.setOnClickListener {
            val selectedOption = getSelectedOption(binding)
            val feedbackText = binding.feedbackInput.text.toString()
            // @TODO: instrumentation
            FeedbackUtil.showMessage(activity, R.string.survey_dialog_submitted_snackbar)
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
        dialog.show()
        Prefs.yearInReviewSurveyState = YearInReviewSurveyState.SHOWN
    }

    private fun getSelectedOption(binding: DialogFeedbackOptionsBinding): Int? {
        val selectedId = binding.feedbackRadioGroup.checkedRadioButtonId
        return when (selectedId) {
            R.id.optionVerySatisfied -> 1
            R.id.optionSatisfied -> 2
            R.id.optionNeutral -> 3
            R.id.optionUnsatisfied -> 4
            R.id.optionVeryUnsatisfied -> 5
            else -> null
        }
    }

    fun resetYearInReviewSurveyState() {
        Prefs.yearInReviewSurveyState = YearInReviewSurveyState.NOT_TRIGGERED
    }
}

enum class YearInReviewSurveyState {
    NOT_TRIGGERED,
    SHOULD_SHOW,
    SHOWN
}
