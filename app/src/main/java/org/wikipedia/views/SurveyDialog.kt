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
        var dialog: AlertDialog? = null
        val feedbackView = activity.layoutInflater.inflate(R.layout.dialog_patrol_edit_feedback_options, null)

        val clickListener = View.OnClickListener {
            val feedbackOption = (it as TextView).text.toString()
            dialog?.dismiss()
            if (feedbackOption == activity.getString(R.string.patroller_diff_feedback_dialog_option_satisfied)) {
                showFeedbackSnackbarAndTooltip(activity, source)
            } else {
                showFeedbackInputDialog(activity, source)
            }

            sendAnalyticsEvent("feedback_selection", "feedback_form", source,
                PatrollerExperienceEvent.getActionDataString(feedbackOption = feedbackOption))
        }

        feedbackView.findViewById<TextView>(R.id.optionSatisfied).setOnClickListener(clickListener)
        feedbackView.findViewById<TextView>(R.id.optionNeutral).setOnClickListener(clickListener)
        feedbackView.findViewById<TextView>(R.id.optionUnsatisfied).setOnClickListener(clickListener)
        sendAnalyticsEvent("impression", "feedback_form", source)
        val dialogBuilder = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.patroller_diff_feedback_dialog_title)
            .setCancelable(false)
            .setView(feedbackView)
        dialog = dialogBuilder.show()
    }

    private fun showFeedbackInputDialog(activity: Activity, source: Constants.InvokeSource) {
        val feedbackView = activity.layoutInflater.inflate(R.layout.dialog_patrol_edit_feedback_input, null)
        sendAnalyticsEvent("impression", "feedback_input_form", source)
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.patroller_diff_feedback_dialog_feedback_title)
            .setView(feedbackView)
            .setPositiveButton(R.string.patroller_diff_feedback_dialog_submit) { _, _ ->
               val feedbackInput = feedbackView.findViewById<TextInputEditText>(R.id.feedbackInput).text.toString()
                sendAnalyticsEvent("feedback_input_submit", "feedback_input_form", source,
                    PatrollerExperienceEvent.getActionDataString(feedbackText = feedbackInput))
                showFeedbackSnackbarAndTooltip(activity, source)
            }
            .show()
    }

    private fun showFeedbackSnackbarAndTooltip(activity: Activity, source: Constants.InvokeSource) {
        FeedbackUtil.showMessage(activity, R.string.patroller_diff_feedback_submitted_snackbar)
        sendAnalyticsEvent("feedback_submit_toast", "feedback_form", source)
        activity.window.decorView.postDelayed({
            val anchorView = activity.findViewById<View>(R.id.more_options)
            if (!activity.isDestroyed && anchorView != null && Prefs.showOneTimeRecentEditsFeedbackForm) {
                sendAnalyticsEvent("tooltip_impression", "feedback_form", source)
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
        if (source == Constants.InvokeSource.SUGGESTED_EDITS_RECENT_EDITS) {
            PatrollerExperienceEvent.logAction(action, activeInterface, actionData)
        }
    }
}
