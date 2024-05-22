package org.wikipedia.feed.view


import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.DialogRegionalLanguageVariantSelectionBinding
import org.wikipedia.databinding.ItemLanguageVariantSelectionBinding
import org.wikipedia.language.AppLanguageLookUpTable

class RegionalLanguageVariantSelectionDialog(context: Context) : MaterialAlertDialogBuilder(context) {
    private lateinit var dialog: AlertDialog
    private var binding = DialogRegionalLanguageVariantSelectionBinding.inflate(LayoutInflater.from(context))
    // TODO: discuss the which one should be the default language code
    private var selectedLanguageCode = AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE

    init {
        setView(binding.root)
        setCancelable(false)
        setPositiveButtonEnabled(false)
        buildRadioButtons()
        setPositiveButton(R.string.feed_language_variants_removal_dialog_save) { _, _ ->
            val list = WikipediaApp.instance.languageState.appLanguageCodes.toMutableList()
            // Remove non-regional language variants before assigning the selected language code
            list.removeAll(nonRegionalLanguageVariants)
            list.add(0, selectedLanguageCode)
            WikipediaApp.instance.languageState.setAppLanguageCodes(list)
        }
    }

    override fun show(): AlertDialog {
        dialog = super.show()
        return dialog
    }

    private fun buildRadioButtons() {
        binding.radioGroup.removeAllViews()
        regionalLanguageVariants.forEach { languageCode ->
            val radioButtonBinding = ItemLanguageVariantSelectionBinding.inflate(LayoutInflater.from(context), binding.radioGroup, false)
            radioButtonBinding.radioButtonTitle.text = WikipediaApp.instance.languageState.getAppLanguageLocalizedName(languageCode)
            radioButtonBinding.radioButtonDescription.text = WikipediaApp.instance.languageState.getAppLanguageCanonicalName(languageCode)
            radioButtonBinding.root.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    setPositiveButtonEnabled(true)
                }
            }
            binding.radioGroup.addView(radioButtonBinding.root)
        }
    }

    private fun setPositiveButtonEnabled(enabled: Boolean) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isEnabled = enabled
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
    }
}
