package org.wikipedia.language

import java.util.Locale

object AcceptLanguageUtil {
    private const val APP_LANGUAGE_QUALITY = .9f
    private const val SYSTEM_LANGUAGE_QUALITY = .8f

    fun getAcceptLanguage(wikiLanguageCode: String, appLanguageCode: String, systemLanguageCode: String): String {
        var acceptLanguage = wikiLanguageCode
        acceptLanguage = appendToAcceptLanguage(acceptLanguage, appLanguageCode, APP_LANGUAGE_QUALITY)
        acceptLanguage = appendToAcceptLanguage(acceptLanguage, systemLanguageCode, SYSTEM_LANGUAGE_QUALITY)
        return acceptLanguage
    }

    private fun appendToAcceptLanguage(acceptLanguage: String, languageCode: String, quality: Float): String {
        if (acceptLanguage.contains(languageCode)) {
            return acceptLanguage
        }
        return if (acceptLanguage.isEmpty()) languageCode else String.format(Locale.ROOT, "%s,%s;q=%.1f", acceptLanguage, languageCode, quality)
    }
}
