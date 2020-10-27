package org.wikipedia.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions

class MlKitLanguageDetector {
    interface Callback {
        fun onLanguageDetectionSuccess(languageCode: String)
    }

    var callback: Callback? = null
    private val languageIdentifier = LanguageIdentification.getClient(LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.65f)
            .build())

    fun detectLanguageFromText(text: String) {
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode: String ->
                    if (languageCode != "und") {
                        callback?.onLanguageDetectionSuccess(languageCode)
                    }
                }
    }
}
