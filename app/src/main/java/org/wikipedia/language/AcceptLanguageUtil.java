package org.wikipedia.language;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class AcceptLanguageUtil {
    private static final float APP_LANGUAGE_QUALITY = .9f;
    private static final float SYSTEM_LANGUAGE_QUALITY = .8f;

    /**
     * @return The value that should go in the Accept-Language header.
     */
    @NonNull
    public static String getAcceptLanguage(@NonNull String wikiLanguageCode,
                                           @NonNull String appLanguageCode,
                                           @NonNull String systemLanguageCode) {
        String acceptLanguage = wikiLanguageCode;
        acceptLanguage = appendToAcceptLanguage(acceptLanguage, appLanguageCode, APP_LANGUAGE_QUALITY);
        acceptLanguage = appendToAcceptLanguage(acceptLanguage, systemLanguageCode, SYSTEM_LANGUAGE_QUALITY);
        return acceptLanguage;
    }

    @NonNull
    private static String appendToAcceptLanguage(@NonNull String acceptLanguage,
                                                 @NonNull String languageCode, float quality) {
        // If accept-language already contains the language, just return accept-language.
        if (acceptLanguage.contains(languageCode)) {
            return acceptLanguage;
        }

        // If accept-language is empty, don't append. Just return the language.
        if (acceptLanguage.isEmpty()) {
            return languageCode;
        }

        // Accept-language is nonempty, append the language.
        return String.format(Locale.ROOT, "%s,%s;q=%.1f", acceptLanguage, languageCode, quality);
    }

    private AcceptLanguageUtil() { }
}
