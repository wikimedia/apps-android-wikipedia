package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.databinding.DialogArticleDescriptionsBinding

class SuggestedArticleDescriptionsDialog(
    context: Context,
    firstSuggestion: String,
    secondSuggestion: String?,
    callback: Callback
) : AlertDialog(context) {

  fun interface Callback {
        fun onSuggestionClicked(suggestion: String)
    }

    private val binding = DialogArticleDescriptionsBinding.inflate(layoutInflater)
    private var suggestionChosen = false

    init {
        setView(binding.root)
        binding.firstSuggestion.text = firstSuggestion
        binding.secondSuggestionLayout.isVisible = secondSuggestion != null
        secondSuggestion?.let { binding.secondSuggestion.text = it }
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.closeButton.setOnClickListener { dismiss() }
        binding.firstSuggestion.setOnClickListener {
            callback.onSuggestionClicked(binding.firstSuggestion.text.toString())
            MachineGeneratedArticleDescriptionsAnalyticsHelper.machineGeneratedSuggestionsDialogSuggestionChosen(context, firstSuggestion)
            suggestionChosen = true
            dismiss()
        }
        binding.secondSuggestion.setOnClickListener {
            callback.onSuggestionClicked(binding.secondSuggestion.text.toString())
            MachineGeneratedArticleDescriptionsAnalyticsHelper.machineGeneratedSuggestionsDialogSuggestionChosen(context, secondSuggestion ?: "")
            suggestionChosen = true
            dismiss()
        }

        binding.firstSuggestionFlag.setOnClickListener {
            SuggestedArticleDescriptionsReportDialog(context, binding.firstSuggestion.text.toString()) { dismiss() }.show()
        }

        binding.secondSuggestionFlag.setOnClickListener {
            SuggestedArticleDescriptionsReportDialog(context, binding.secondSuggestion.text.toString()) { dismiss() }.show()
        }

        setOnDismissListener {
            if (!suggestionChosen) {
                MachineGeneratedArticleDescriptionsAnalyticsHelper.machineGeneratedSuggestionsDialogOptedOut(context)
            }
        }
    }

    companion object {
        fun availableLanguages(): List<String> {
            return listOf(
                "en", "ru", "vi", "ja", "de", "ro", "fr", "fi", "ko", "es", "zh", "it",
                "nl", "ar", "tr", "hi", "cs", "lt", "lv", "kk", "et", "ni", "si", "gu", "my"
            )
        }
    }
}
