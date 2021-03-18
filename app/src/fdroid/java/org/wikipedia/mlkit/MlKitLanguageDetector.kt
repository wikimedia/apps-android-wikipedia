package org.wikipedia.mlkit

class MlKitLanguageDetector {
    interface Callback {
        fun onLanguageDetectionSuccess(languageCode: String)
    }

    var callback: Callback? = null

    fun detectLanguageFromText(text: String) {
        // stub
    }
}
