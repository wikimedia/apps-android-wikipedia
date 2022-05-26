package org.wikipedia.mlkit

import androidx.fragment.app.Fragment
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import org.wikipedia.WikipediaApp

class MlKitTextTranslation {
    fun interface Callback {
        fun onTextTranslated(text: String)
    }

    private val languageIdentifier = LanguageIdentification.getClient(LanguageIdentificationOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .build())

    fun translateText(fragment: Fragment, text: String, callback: Callback) {
        // TODO: optimize this nested listeners
        languageIdentifier.identifyPossibleLanguages(text)
                .addOnSuccessListener { languageCodes ->
                    languageCodes.sortByDescending { it.confidence }
                    val topConfidence = languageCodes.first()

                    if (WikipediaApp.instance.appOrSystemLanguageCode.equals(topConfidence.languageTag, true)) {
                        return@addOnSuccessListener
                    }

                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(topConfidence.languageTag)
                        .setTargetLanguage(WikipediaApp.instance.appOrSystemLanguageCode)
                        .build()
                    val translator = Translation.getClient(options)
                    val conditions = DownloadConditions.Builder()
                        .requireWifi()
                        .build()

                    fragment.lifecycle.addObserver(translator)
                    translator.downloadModelIfNeeded(conditions)
                        .addOnSuccessListener {
                            translator.translate(text)
                                .addOnSuccessListener { translatedText ->
                                    callback.onTextTranslated(translatedText)
                                }
                        }
                }
    }
}
