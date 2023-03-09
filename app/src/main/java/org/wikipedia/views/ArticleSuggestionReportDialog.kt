package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.databinding.DialogDescriptionSuggestionReportBinding
import org.wikipedia.util.FeedbackUtil

class ArticleSuggestionReportDialog(context: Context, suggestion: String, callback: Callback) : AlertDialog(context) {

    interface Callback {
        fun onReportClick()
    }

    private var reported = false
    private val binding = DialogDescriptionSuggestionReportBinding.inflate(layoutInflater)

    init {
        setView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.reportButton.setOnClickListener {
            collectReportData(suggestion)
            FeedbackUtil.makeSnackbar(context as Activity,
                context.getString(R.string.suggested_edits_suggestion_report_submitted)).show()
            callback.onReportClick()
            reported = true
            dismiss()
        }
        binding.suggestionReportOther.setEndIconOnClickListener {
            binding.suggestionReportOther.editText?.text?.clear()
        }
        binding.cancelButton.setOnClickListener { dismiss() }
        setOnDismissListener {
            if (!reported) {
                MachineGeneratedArticleDescriptionsAnalyticsHelper.logReportDialogCancelled(context,
                    suggestion, getReportReasons())
            }
        }
    }

    private fun getReportReasons(): MutableList<String> {
        val responses = mutableListOf<String>()

        if (binding.notEnoughInfoCheckbox.isChecked) {
            responses.add(binding.notEnoughInfoCheckbox.text.toString())
        }
        if (binding.cannotSeeDescriptionCheckbox.isChecked) {
            responses.add(binding.cannotSeeDescriptionCheckbox.text.toString())
        }
        if (binding.doNotUnderstandCheckbox.isChecked) {
            responses.add(binding.doNotUnderstandCheckbox.text.toString())
        }
        if (binding.inappropriateSuggestionCheckbox.isChecked) {
            responses.add(binding.inappropriateSuggestionCheckbox.text.toString())
        }
        responses.add(binding.suggestionReportOther.editText?.text.toString())
        return responses
    }

    private fun collectReportData(suggestion: String) {
        MachineGeneratedArticleDescriptionsAnalyticsHelper.logSuggestionReported(context,
            suggestion, getReportReasons())
    }
}
