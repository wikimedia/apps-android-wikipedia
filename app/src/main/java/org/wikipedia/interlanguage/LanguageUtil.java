package org.wikipedia.interlanguage;

import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class LanguageUtil {
    private static final String HONG_KONG_COUNTRY_CODE = "HK";
    private static final String MACAU_COUNTRY_CODE = "MO";
    private static final List<String> TRADITIONAL_CHINESE_COUNTRY_CODES = Arrays.asList(
            Locale.TAIWAN.getCountry(), HONG_KONG_COUNTRY_CODE, MACAU_COUNTRY_CODE);

    /**
     * Takes a ISO language code (as returned by Android) and returns a wiki code, as used by wikipedia.
     *
     * @param code Language code (as returned by Android)
     * @return Wiki code, as used by wikipedia.
     */
    public static String languageCodeToWikiLanguageCode(String code) {
        // Convert deprecated language codes to modern ones.
        // See https://developer.android.com/reference/java/util/Locale.html
        switch (code) {
            case "iw":
                return "he"; // Hebrew
            case "in":
                return "id"; // Indonesian
            case "ji":
                return "yi"; // Yiddish
            case "zh":
                // If the user configures Android for Chinese (zh) we have no way to know the intended
                // dialect. We guess based on country. If the guess is incorrect, the user must explicitly
                // choose the dialect in the app settings.
                return isTraditionalChinesePredominantInCountry(Locale.getDefault().getCountry())
                        ? AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE
                        : AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
            default:
                return code;
        }
    }

    private static boolean isTraditionalChinesePredominantInCountry(@Nullable String country) {
        return TRADITIONAL_CHINESE_COUNTRY_CODES.contains(country);
    }

    private LanguageUtil() { }
}