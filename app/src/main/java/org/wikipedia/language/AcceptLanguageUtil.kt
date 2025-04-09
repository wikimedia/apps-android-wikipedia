package org.wikipedia.language

import org.wikipedia.WikipediaApp
import java.util.Locale

object AcceptLanguageUtil {

    fun getAcceptLanguage(wikiLanguageCode: String, appLanguageCode: String, systemLanguageCode: String): String {
        val languageState = WikipediaApp.instance.languageState
        var quality = 1f

        // Compose a list of accepted languages, with descending quality values.
        // The first language is the most preferred, and has an implicit quality of 1.0.
        var acceptLanguage = wikiLanguageCode
        quality -= 0.1f

        if (wikiLanguageCode.startsWith("zh")) {
            // https://phabricator.wikimedia.org/T359822#10402218
            // For the specific case of Chinese, we provide the old-style language variant code,
            // in addition to the new-style BCP code. This seems to fix a tricky issue with the
            // mobile-html endpoint, and potentially others that use old-style variant codes.
            acceptLanguage = appendToAcceptLanguage(acceptLanguage, languageState.getBcp47LanguageCode(wikiLanguageCode), quality)
            quality -= 0.1f
        } else {
            acceptLanguage = languageState.getBcp47LanguageCode(wikiLanguageCode)
        }

        acceptLanguage = appendToAcceptLanguage(acceptLanguage, languageState.getBcp47LanguageCode(appLanguageCode), quality)
        quality -= 0.1f

        acceptLanguage = appendToAcceptLanguage(acceptLanguage, languageState.getBcp47LanguageCode(systemLanguageCode), quality)
        return acceptLanguage
    }

    private fun appendToAcceptLanguage(acceptLanguage: String, languageCode: String, quality: Float): String {
        if (acceptLanguage.contains(languageCode)) {
            return acceptLanguage
        }
        return if (acceptLanguage.isEmpty()) languageCode else String.format(Locale.ROOT, "%s,%s;q=%.1f", acceptLanguage, languageCode, quality)
    }
}
