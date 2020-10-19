package org.wikipedia.mlkit

import com.google.mlkit.nl.languageid.LanguageIdentification

class MlKitLanguageDetector {
    interface Callback {
        fun onLanguageDetectionSuccess(languageCode: String)
    }

    var callback: Callback? = null
    fun detectLanguageFromText(text: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode: String ->
                    if (languageCode != "und") {
                        callback?.onLanguageDetectionSuccess(languageCode)
                    }
                }
    }
}
