package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.databinding.DialogDescriptionSuggestionReportBinding
import org.wikipedia.page.PageTitle
import org.wikipedia.util.FeedbackUtil

class SuggestedArticleDescriptionsReportDialog(
    context: Context,
    suggestion: String,
    private val pageTitle: PageTitle,
    private val analyticsHelper: MachineGeneratedArticleDescriptionsAnalyticsHelper,
    callback: Callback
) : AlertDialog(context) {

    fun interface Callback {
        fun onReportClick()
    }

    private var reported = false
    private val binding = DialogDescriptionSuggestionReportBinding.inflate(layoutInflater)

    init {
        setView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.reportButton.setOnClickListener {
            if (getReportReasons().isNotEmpty()) {
                analyticsHelper.logSuggestionReported(context, suggestion, getReportReasons(), pageTitle)
                FeedbackUtil.makeSnackbar(context as Activity, context.getString(R.string.suggested_edits_suggestion_report_submitted)).show()
                callback.onReportClick()
                reported = true
                dismiss()
            }
        }
        binding.suggestionReportOther.setEndIconOnClickListener {
            binding.suggestionReportOther.editText?.text?.clear()
        }
        binding.cancelButton.setOnClickListener { dismiss() }
        setOnDismissListener {
            if (!reported) {
                analyticsHelper.logReportDialogDismissed(context)
            }
        }
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
