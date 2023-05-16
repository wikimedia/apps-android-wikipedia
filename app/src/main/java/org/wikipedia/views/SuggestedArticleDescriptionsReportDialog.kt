package org.wikipedia.views

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.databinding.DialogDescriptionSuggestionReportBinding
import org.wikipedia.page.PageTitle
import org.wikipedia.util.FeedbackUtil

class SuggestedArticleDescriptionsReportDialog(
    activity: Activity,
    suggestion: String,
    private val pageTitle: PageTitle,
    private val analyticsHelper: MachineGeneratedArticleDescriptionsAnalyticsHelper,
    callback: Callback
) : MaterialAlertDialogBuilder(activity) {

    fun interface Callback {
        fun onReportClick()
    }

    private var reported = false
    private val binding = DialogDescriptionSuggestionReportBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null

    init {
        setView(binding.root)

        setPositiveButton(context.getString(R.string.suggested_edits_report_suggestion)) { _, _ ->
            if (getReportReasons().isNotEmpty()) {
                analyticsHelper.logSuggestionReported(context, suggestion, getReportReasons(), pageTitle)
                FeedbackUtil.makeSnackbar(activity, context.getString(R.string.suggested_edits_suggestion_report_submitted)).show()
                callback.onReportClick()
                reported = true
                dialog?.dismiss()
            }
        }

        setNegativeButton(context.getString(R.string.text_input_dialog_cancel_button_text)) { _, _ ->
            dialog?.dismiss()
        }

        binding.suggestionReportOther.setEndIconOnClickListener {
            binding.suggestionReportOther.editText?.text?.clear()
        }
        setOnDismissListener {
            if (!reported) {
                analyticsHelper.logReportDialogDismissed(context)
            }
        }
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    private fun getReportReasons(): List<String> {
        val responses = mutableListOf<String>()

        if (binding.notEnoughInfo.isChecked) {
            responses.add(context.resources.getResourceEntryName(binding.notEnoughInfo.id))
        }
        if (binding.cannotSeeDescription.isChecked) {
            responses.add(context.resources.getResourceEntryName(binding.cannotSeeDescription.id))
        }
        if (binding.doNotUnderstand.isChecked) {
            responses.add(context.resources.getResourceEntryName(binding.doNotUnderstand.id))
        }
        if (binding.inappropriateSuggestion.isChecked) {
            responses.add(context.resources.getResourceEntryName(binding.inappropriateSuggestion.id))
        }
        val enteredText = binding.suggestionReportOther.editText?.text?.toString()
        if (!enteredText.isNullOrEmpty()) responses.add(enteredText)
        return responses
    }
}
