package org.wikipedia.feed.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.DialogRegionalLanguageVariantSelectionBinding
import org.wikipedia.databinding.ItemLanguageVariantSelectionBinding
import org.wikipedia.language.AppLanguageLookUpTable

class RegionalLanguageVariantSelectionDialog(context: Context) : MaterialAlertDialogBuilder(context) {
    private var dialog: AlertDialog? = null
    private var binding = DialogRegionalLanguageVariantSelectionBinding.inflate(LayoutInflater.from(context))
    private var selectedLanguageCode = AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE

    init {
        setView(binding.root)
        setCancelable(false)
        buildRadioButtons(context)
        setPositiveButton(R.string.feed_language_variants_removal_dialog_save) { _, _ ->
            val list = removeNonRegionalLanguageVariants()
            list.remove(selectedLanguageCode) // Remove the existing one to add it to the top
            list.add(0, selectedLanguageCode)
            WikipediaApp.instance.languageState.setAppLanguageCodes(list)
        }
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        setPositiveButtonEnabled(false)
        return dialog!!
    }

    private fun buildRadioButtons(context: Context) {
        regionalLanguageVariants.forEach { languageCode ->
            val radioButtonBinding = ItemLanguageVariantSelectionBinding.inflate(LayoutInflater.from(context))
            radioButtonBinding.root.tag = languageCode
            radioButtonBinding.radioButtonTitle.text = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(languageCode)
            radioButtonBinding.radioButtonDescription.text = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(languageCode)
            radioButtonBinding.radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedLanguageCode = languageCode
                    setPositiveButtonEnabled(true)
                    clearCheckedButtons()
                }
            }
            binding.radioGroup.addView(radioButtonBinding.root)
        }
    }

    private fun clearCheckedButtons() {
        binding.radioGroup.children.iterator().forEach {
            val radioButton = it.findViewById<RadioButton>(R.id.radioButton)
            radioButton.isChecked = selectedLanguageCode == it.tag
        }
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
    }

    companion object {

        private val nonRegionalLanguageVariants = listOf(
            AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
            AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE
        )

        private val regionalLanguageVariants = listOf(
            AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_HK_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_MO_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_MY_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_SG_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE
        )

        fun removeNonRegionalLanguageVariants(): MutableList<String> {
            val list = WikipediaApp.instance.languageState.appLanguageCodes.toMutableList()
            list.removeAll(nonRegionalLanguageVariants)
            return list
        }
    }
}
