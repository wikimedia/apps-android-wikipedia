package org.wikipedia.views

import android.app.Activity
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.analytics.eventplatform.RabbitHolesEvent
import org.wikipedia.databinding.DialogFeedbackOptionsBinding
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil

object SurveyDialog {

    fun showFeedbackOptionsDialog(activity: Activity,
                                  titleId: Int = R.string.survey_dialog_title,
                                  messageId: Int = R.string.survey_dialog_message,
                                  snackbarMessageId: Int = R.string.survey_dialog_submitted_snackbar,
                                  invokeSource: Constants.InvokeSource) {
        var dialog: AlertDialog? = null
        val binding = DialogFeedbackOptionsBinding.inflate(activity.layoutInflater)
        binding.titleText.text = activity.getString(titleId)
        binding.messageText.text = activity.getString(messageId)
        binding.feedbackInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.dialogContainer.postDelayed({
                    binding.dialogContainer.fullScroll(ScrollView.FOCUS_DOWN)
                }, 200)
            }
        }

        if (invokeSource == Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS) {
            val clickListener = View.OnClickListener {
                val feedbackOption = (it as TextView).text.toString()
                dialog?.dismiss()
                if (feedbackOption == activity.getString(R.string.survey_dialog_option_satisfied)) {
                    showFeedbackSnackbarAndTooltip(activity, snackbarMessageId, invokeSource)
                } else {
                    showFeedbackInputDialog(activity, snackbarMessageId, invokeSource)
                }

                PatrollerExperienceEvent.logAction("feedback_selection", "feedback_form",
                    PatrollerExperienceEvent.getActionDataString(feedbackOption = feedbackOption))
            }
            binding.optionSatisfied.setOnClickListener(clickListener)
            binding.optionNeutral.setOnClickListener(clickListener)
            binding.optionUnsatisfied.setOnClickListener(clickListener)

            PatrollerExperienceEvent.logAction("impression", "feedback_form")
        } else if (invokeSource == Constants.InvokeSource.RABBIT_HOLE_SEARCH || invokeSource == Constants.InvokeSource.RABBIT_HOLE_READING_LIST) {
            binding.optionNeutral.isChecked = true
            binding.feedbackInputContainer.isVisible = true

            RabbitHolesEvent.submit("impression", "feedback_survey")
        }

        val dialogBuilder = MaterialAlertDialogBuilder(activity, R.style.AlertDialogTheme_AdjustResize)
            .setCancelable(false)
            .setView(binding.root)

        if (invokeSource == Constants.InvokeSource.RABBIT_HOLE_SEARCH || invokeSource == Constants.InvokeSource.RABBIT_HOLE_READING_LIST) {
            binding.submitButton.setOnClickListener {
                val feedbackInput = binding.feedbackInput.text.toString()

                RabbitHolesEvent.submit("submit_click", "feedback_survey",
                    feedbackSelect = if (binding.optionSatisfied.isChecked) "satisfied"
                    else if (binding.optionUnsatisfied.isChecked) "unsatisfied"
                    else "neutral",
                    feedbackText = feedbackInput)

                showFeedbackSnackbarAndTooltip(activity, snackbarMessageId, invokeSource)
                dialog?.dismiss()
            }
            binding.cancelButton.setOnClickListener {
                dialog?.dismiss()
            }
        }

        dialog = dialogBuilder.show()
    }

    private fun showFeedbackInputDialog(activity: Activity, messageId: Int, source: Constants.InvokeSource) {
        val feedbackView = activity.layoutInflater.inflate(R.layout.dialog_feedback_input, null)
        PatrollerExperienceEvent.logAction("impression", "feedback_input_form")
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.patroller_diff_feedback_dialog_feedback_title)
            .setView(feedbackView)
            .setPositiveButton(R.string.survey_dialog_submit) { _, _ ->
               val feedbackInput = feedbackView.findViewById<TextInputEditText>(R.id.feedbackInput).text.toString()
                PatrollerExperienceEvent.logAction("feedback_input_submit", "feedback_input_form",
                    PatrollerExperienceEvent.getActionDataString(feedbackText = feedbackInput))
                showFeedbackSnackbarAndTooltip(activity, messageId, source)
            }
            .show()
    }

    private fun showFeedbackSnackbarAndTooltip(activity: Activity, messageId: Int, source: Constants.InvokeSource) {
        FeedbackUtil.showMessage(activity, messageId)
        when (source) {
            Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS -> {
                PatrollerExperienceEvent.logAction("feedback_submit_toast", "feedback_form")
                activity.window.decorView.postDelayed({
                    val anchorView = activity.findViewById<View>(R.id.more_options)
                    if (!activity.isDestroyed && anchorView != null && Prefs.showOneTimeRecentEditsFeedbackForm) {
                        PatrollerExperienceEvent.logAction("tooltip_impression", "feedback_form")
                        FeedbackUtil.getTooltip(
                            activity,
                            activity.getString(R.string.patroller_diff_feedback_tooltip),
                            arrowAnchorPadding = -DimenUtil.roundedDpToPx(7f),
                            topOrBottomMargin = 0,
                            aboveOrBelow = false,
                            autoDismiss = false,
                            showDismissButton = true
                        ).apply {
                            showAlignBottom(anchorView)
                            when (source) {
                                Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS -> {
                                    Prefs.showOneTimeRecentEditsFeedbackForm = false
                                }
                                else -> { }
                            }
                        }
                    }
                }, 100)
            }
            else -> {}
        }
    }
}
