package org.wikipedia.views

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.databinding.DialogArticleDescriptionsBinding
import org.wikipedia.page.PageTitle

class SuggestedArticleDescriptionsDialog(
    activity: Activity,
    firstSuggestion: String,
    secondSuggestion: String?,
    private val pageTitle: PageTitle,
    private val analyticsHelper: MachineGeneratedArticleDescriptionsAnalyticsHelper,
    callback: Callback
) : MaterialAlertDialogBuilder(activity) {

    fun interface Callback {
        fun onSuggestionClicked(suggestion: String)
    }

    private val binding = DialogArticleDescriptionsBinding.inflate(activity.layoutInflater)
    private var dialog: AlertDialog? = null
    private var suggestionChosen = false

    init {
        setView(binding.root)
        binding.firstSuggestion.text = firstSuggestion
        binding.secondSuggestionLayout.isVisible = secondSuggestion != null
        secondSuggestion?.let { binding.secondSuggestion.text = it }

        binding.closeButton.setOnClickListener { dialog?.dismiss() }
        binding.firstSuggestion.setOnClickListener {
            callback.onSuggestionClicked(binding.firstSuggestion.text.toString())
            suggestionChosen = true
            dialog?.dismiss()
        }
        binding.secondSuggestion.setOnClickListener {
            callback.onSuggestionClicked(binding.secondSuggestion.text.toString())
            suggestionChosen = true
            dialog?.dismiss()
        }

        binding.firstSuggestionFlag.setOnClickListener {
            SuggestedArticleDescriptionsReportDialog(activity, binding.firstSuggestion.text.toString(), pageTitle, analyticsHelper) { dialog?.dismiss() }.show()
        }

        binding.secondSuggestionFlag.setOnClickListener {
            SuggestedArticleDescriptionsReportDialog(activity, binding.secondSuggestion.text.toString(), pageTitle, analyticsHelper) { dialog?.dismiss() }.show()
        }

        setOnDismissListener {
            if (!suggestionChosen) {
                analyticsHelper.logSuggestionsDismissed(context, pageTitle)
            }
        }
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog!!
    }

    companion object {
        // TODO: reenable 'en' when ready.
        val availableLanguages = listOf(
            "ru", "vi", "ja", "de", "ro", "fr", "fi", "ko", "es", "zh", "it",
            "nl", "ar", "tr", "hi", "cs", "lt", "lv", "kk", "et", "ni", "si", "gu", "my"
        )
    }
}
