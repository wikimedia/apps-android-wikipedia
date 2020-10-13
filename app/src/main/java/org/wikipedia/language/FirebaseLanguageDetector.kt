package org.wikipedia.language

import com.google.mlkit.nl.languageid.LanguageIdentification

class FirebaseLanguageDetector {
    interface Callback {
        fun onLanguageDetectionSuccess(languageCode: String)
    }

    var callback: Callback? = null
    fun detectLanguageFromText(text: String) {
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(text)
                .addOnSuccessListener { languageCode: String ->
                    if (callback != null) {
                        callback!!.onLanguageDetectionSuccess(languageCode)
                    }
                }
    }
}