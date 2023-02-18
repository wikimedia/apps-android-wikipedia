package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wikipedia.databinding.DialogArticleDescriptionsBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.descriptions.DescriptionSuggestionService
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L

class ArticleDescriptionsDialog(context: Context,
                                var pageTitle: PageTitle,
                                callback: Callback) : AlertDialog(context) {

    interface Callback {
        fun onSuggestionClicked(suggestion: String)
    }

    private val binding = DialogArticleDescriptionsBinding.inflate(layoutInflater)

    init {
        binding.circularProgressBar.isVisible = true
        binding.descriptionsContainer.isVisible = false
        setView(binding.root)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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
            ArticleSuggestionReportDialog(context, binding.firstSuggestion.text.toString()).show()
            dismiss()
        }

        binding.secondSuggestionFlag.setOnClickListener {
            ArticleSuggestionReportDialog(context, binding.firstSuggestion.text.toString()).show()
            dismiss()
        }
        requestSuggestion()
    }

    private fun requestSuggestion() {
        lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
        }) {
            withContext(Dispatchers.IO) {
                val response = ServiceFactory[pageTitle.wikiSite, DescriptionSuggestionService.API_URL, DescriptionSuggestionService::class.java]
                    .getSuggestion(pageTitle.wikiSite.languageCode, pageTitle.prefixedText, 2)

                // Perform some post-processing on the predictions.
                // 1) Capitalize them, if we're dealing with enwiki.
                // 2) Remove duplicates.
                val list = (if (pageTitle.wikiSite.languageCode == "en") {
                    response.prediction.map { StringUtil.capitalize(it)!! }
                } else response.prediction).distinct()
                updateUI(list)
            }
        }
    }

    private fun updateUI(list: List<String>) {
        binding.root.post {
            binding.firstSuggestion.text = list.first()
            binding.secondSuggestion.text = list.last()
            binding.circularProgressBar.isVisible = false
            binding.descriptionsContainer.isVisible = true
        }
    }
}
