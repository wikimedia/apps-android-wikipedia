package org.wikipedia.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import org.wikipedia.language.AppLanguageLookUpTable

class MlKitLanguageDetector {
    interface Callback {
        fun onLanguageDetectionSuccess(languageCodes: List<String>)
    }

    var callback: Callback? = null
    private val languageIdentifier = LanguageIdentification.getClient(LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build())

    fun detectLanguageFromText(text: String) {
        languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener { languageCodes ->
                    val languageStrings = languageCodes.map { it.languageTag }.filter { it != "und" }.toMutableList()
                    if (languageStrings.contains(AppLanguageLookUpTable.NORWEGIAN_LEGACY_LANGUAGE_CODE)) {
                        // Manually add Norwegian Bokmål if regular Norwegian is detected. (T302731)
                        languageStrings.add(AppLanguageLookUpTable.NORWEGIAN_BOKMAL_LANGUAGE_CODE)
                    }
                    if (languageStrings.isNotEmpty()) {
                        callback?.onLanguageDetectionSuccess(languageStrings)
                    }
                }
    }
}
