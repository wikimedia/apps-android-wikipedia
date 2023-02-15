package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import org.wikipedia.databinding.DialogArticleDescriptionsBinding

class ArticleDescriptionsDialog(context: Context,
                                firstDescriptionSuggestion: String?,
                                secondDescriptionSuggestion: String?,
                                callback: Callback) : AlertDialog(context) {

    interface Callback {
        fun onSuggestionClicked(suggestion: String)
    }

    private val binding = DialogArticleDescriptionsBinding.inflate(layoutInflater)

    init {
        setView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.closeButton.setOnClickListener { dismiss() }
        binding.firstSuggestion.setOnClickListener {
            firstDescriptionSuggestion?.let { callback.onSuggestionClicked(it) }
            dismiss()
        }
        binding.secondSuggestion.setOnClickListener {
            secondDescriptionSuggestion?.let { callback.onSuggestionClicked(it) }
            dismiss()
        }

        binding.firstSuggestionFlag.setOnClickListener {
            ArticleSuggestionReportDialog(context, firstDescriptionSuggestion!!).show()
            dismiss()
        }

        binding.secondSuggestionFlag.setOnClickListener {
            ArticleSuggestionReportDialog(context, secondDescriptionSuggestion!!).show()
            dismiss()
        }

        binding.firstSuggestion.text = firstDescriptionSuggestion
        binding.secondSuggestion.text = secondDescriptionSuggestion
    }
}
