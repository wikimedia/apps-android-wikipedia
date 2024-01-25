package org.wikipedia.views

import android.app.Activity
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil

object SurveyDialog {

    fun showFeedbackOptionsDialog(activity: Activity, source: Constants.InvokeSource) {
        sendAnalyticsEvent("toolbar_first_feedback", "pt_feedback", source)
        var dialog: AlertDialog? = null
        val feedbackView = activity.layoutInflater.inflate(R.layout.dialog_patrol_edit_feedback_options, null)

        val clickListener = View.OnClickListener {
            val feedbackOption = (it as TextView).text.toString()
            dialog?.dismiss()
            if (
                (source == Constants.InvokeSource.PLACES && (feedbackOption == activity.getString(R.string.patroller_diff_feedback_dialog_option_satisfied) ||
                        feedbackOption == activity.getString(R.string.places_survey_very_satisfied_option))) ||
                (source != Constants.InvokeSource.PLACES && feedbackOption == activity.getString(R.string.patroller_diff_feedback_dialog_option_satisfied))
            ) {
                showFeedbackSnackbarAndTooltip(activity, source)
            } else {
                showFeedbackInputDialog(activity, source, feedbackOption)
            }

            sendAnalyticsEvent("feedback_selection", "pt_feedback", source,
                PatrollerExperienceEvent.getActionDataString(feedbackOption = feedbackOption))
        }

        feedbackView.findViewById<TextView>(R.id.optionSatisfied).setOnClickListener(clickListener)
        feedbackView.findViewById<TextView>(R.id.optionNeutral).setOnClickListener(clickListener)
        feedbackView.findViewById<TextView>(R.id.optionUnsatisfied).setOnClickListener(clickListener)
        if (source == Constants.InvokeSource.PLACES) {
            val verySatisfied = feedbackView.findViewById<TextView>(R.id.optionVerySatisfied)
            val veryUnsatisfied = feedbackView.findViewById<TextView>(R.id.optionVeryUnsatisfied)
            verySatisfied.visibility = View.VISIBLE
            veryUnsatisfied.visibility = View.VISIBLE
            verySatisfied.setOnClickListener(clickListener)
            veryUnsatisfied.setOnClickListener(clickListener)
        }
        PatrollerExperienceEvent.logImpression("pt_feedback")
        val dialogBuilder = MaterialAlertDialogBuilder(activity)
            .setTitle(
                if (source == Constants.InvokeSource.PLACES) R.string.places_survey_dialog_title else
                    R.string.patroller_diff_feedback_dialog_title
            )
            .setCancelable(false)
            .setView(feedbackView)
        if (source == Constants.InvokeSource.PLACES) {
            dialogBuilder.setNegativeButton(R.string.logged_out_in_background_cancel) { _, _ ->
                dialog?.dismiss()
                sendAnalyticsEvent("feedback_cancel", "pt_feedback", source)
            }
        }
        dialog = dialogBuilder.show()
    }

    private fun showFeedbackInputDialog(activity: Activity, source: Constants.InvokeSource, feedbackOption: String) {
        val feedbackView = activity.layoutInflater.inflate(R.layout.dialog_patrol_edit_feedback_input, null)
        sendAnalyticsEvent("feedback_input_impression", "pt_feedback", source)
        MaterialAlertDialogBuilder(activity)
            .setTitle(
                if (source == Constants.InvokeSource.PLACES) {
                    if (feedbackOption == activity.getString(R.string.places_survey_very_unsatisfied_option) ||
                        feedbackOption == activity.getString(R.string.patroller_diff_feedback_dialog_option_unsatisfied)
                    )
                        R.string.places_survey_feedback_low_satisfaction_dialog_title
                    else
                        R.string.places_survey_feedback_dialog_title
                } else {
                    R.string.patroller_diff_feedback_dialog_feedback_title
                }
            )
            .setView(feedbackView)
            .setPositiveButton(R.string.patroller_diff_feedback_dialog_submit) { _, _ ->
               val feedbackInput = feedbackView.findViewById<TextInputEditText>(R.id.feedbackInput).text.toString()
                sendAnalyticsEvent("feedback_submit", "pt_feedback", source,
                    PatrollerExperienceEvent.getActionDataString(feedbackText = feedbackInput))
                showFeedbackSnackbarAndTooltip(activity, source)
            }
            .show()
    }

    private fun showFeedbackSnackbarAndTooltip(activity: Activity, source: Constants.InvokeSource) {
        FeedbackUtil.showMessage(activity, R.string.patroller_diff_feedback_submitted_snackbar)
        sendAnalyticsEvent("feedback_submit_toast", "pt_feedback", source)
        activity.window.decorView.postDelayed({
            val anchorView = activity.findViewById<View>(R.id.more_options)
            if (!activity.isDestroyed && anchorView != null && Prefs.showOneTimeRecentEditsFeedbackForm) {
                sendAnalyticsEvent("tooltip_impression", "pt_feedback", source)
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
                    Prefs.showOneTimeRecentEditsFeedbackForm = false
                }
            }
        }, 100)
    }

    private fun sendAnalyticsEvent(action: String, activeInterface: String, source: Constants.InvokeSource, actionData: String = "") {
        if (Prefs.showOneTimeRecentEditsFeedbackForm && source == Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS) {
            PatrollerExperienceEvent.logAction(action, activeInterface, actionData)
        }
    }
}
