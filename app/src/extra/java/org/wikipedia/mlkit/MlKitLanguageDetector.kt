package org.wikipedia.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions

class MlKitLanguageDetector {
    interface Callback {
        fun onLanguageDetectionSuccess(languageCode: String)
    }

    var callback: Callback? = null
    fun detectLanguageFromText(text: String) {
        val languageIdentifier = LanguageIdentification.getClient(LanguageIdentificationOptions.Builder()
                .setConfidenceThreshold(0.65f)
                .build())
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode: String ->
                    if (languageCode != "und") {
                        callback?.onLanguageDetectionSuccess(languageCode)
                    }
                }
    }
}
