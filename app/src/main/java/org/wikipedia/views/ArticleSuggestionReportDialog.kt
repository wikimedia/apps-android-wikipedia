package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import org.wikipedia.R
import org.wikipedia.databinding.DialogDescriptionSuggestionReportBinding
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.views.ArticleDescriptionsDialog.Callback

class ArticleSuggestionReportDialog(context: Context, suggestion: String, callback: Callback) : AlertDialog(context) {

    interface Callback {
        fun onReportClick()
    }

    private val binding = DialogDescriptionSuggestionReportBinding.inflate(layoutInflater)

    init {
        setView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.closeButton.setOnClickListener { dismiss() }
        binding.reportButton.setOnClickListener {
            collectReportData()
            FeedbackUtil.makeSnackbar(context as Activity,
                context.getString(R.string.suggested_edits_suggestion_report_submitted)).show()
            callback.onReportClick()
            dismiss()
        }
        binding.suggestionReportOther.setEndIconOnClickListener {
            binding.suggestionReportOtherEditText.setText("")
        }
        binding.cancelButton.setOnClickListener { dismiss() }
    }

    private fun collectReportData() {
        // Todo: Implement during analytics wiring
    }
}
