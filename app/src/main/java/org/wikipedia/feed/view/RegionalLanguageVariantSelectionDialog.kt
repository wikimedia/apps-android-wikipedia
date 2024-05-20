package org.wikipedia.feed.view

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.databinding.DialogRegionalLanguageVariantSelectionBinding
import org.wikipedia.language.AppLanguageLookUpTable

class RegionalLanguageVariantSelectionDialog(context: Context) : MaterialAlertDialogBuilder(context) {

    private var binding = DialogRegionalLanguageVariantSelectionBinding.inflate(LayoutInflater.from(context))

    init {
        setView(binding.root)
        setCancelable(false)
        setPositiveButton(R.string.feed_language_variants_removal_dialog_save) { _, _ ->
        }
    }

    companion object {
        val regionalLanguageVariants = listOf(
            AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_HK_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_MO_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_MY_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_SG_LANGUAGE_CODE,
            AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE
        )
    }
}
