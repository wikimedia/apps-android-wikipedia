package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.databinding.DialogArticleDescriptionsBinding

class ArticleDescriptionsDialog(
    context: Context,
    firstSuggestion: String,
    secondSuggestion: String,
    callback: Callback
) : AlertDialog(context) {

    interface Callback {
        fun onSuggestionClicked(suggestion: String)
    }

    private val binding = DialogArticleDescriptionsBinding.inflate(layoutInflater)

    init {
        setView(binding.root)
        binding.firstSuggestion.text = firstSuggestion
        binding.secondSuggestion.text = secondSuggestion
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        MachineGeneratedArticleDescriptionsAnalyticsHelper.machineGeneratedSuggestionsDialogShown(context)

        binding.closeButton.setOnClickListener { dismiss() }
        binding.firstSuggestion.setOnClickListener {
            callback.onSuggestionClicked(binding.firstSuggestion.text.toString())
            dismiss()
        }
        binding.secondSuggestion.setOnClickListener {
            callback.onSuggestionClicked(binding.secondSuggestion.text.toString())
            dismiss()
        }

        binding.firstSuggestionFlag.setOnClickListener {
            ArticleSuggestionReportDialog(context, binding.firstSuggestion.text.toString(), object :
                ArticleSuggestionReportDialog.Callback {
                override fun onReportClick() {
                    dismiss()
                }
            }).show()
        }

        binding.secondSuggestionFlag.setOnClickListener {
            ArticleSuggestionReportDialog(context, binding.firstSuggestion.text.toString(), object :
                ArticleSuggestionReportDialog.Callback {
                override fun onReportClick() {
                    dismiss()
                }
            }).show()
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
