package org.wikipedia.language;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.LocaleListCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class LanguageUtil {
    private static final String HONG_KONG_COUNTRY_CODE = "HK";
    private static final String MACAU_COUNTRY_CODE = "MO";
    private static final List<String> TRADITIONAL_CHINESE_COUNTRY_CODES = Arrays.asList(
            Locale.TAIWAN.getCountry(), HONG_KONG_COUNTRY_CODE, MACAU_COUNTRY_CODE);

    /**
     * Gets a list of language codes currently enabled by the user.
     * Guarantees at least one language code returned.
     * @return List of language codes pluggable into WikiSite.
     */
    @NonNull public static List<String> getAvailableLanguages() {
        List<String> languages = new ArrayList<>();
        LocaleListCompat localeList = LocaleListCompat.getDefault();
        for (int i = 0; i < localeList.size(); i++) {
            languages.add(localeToWikiLanguageCode(localeList.get(i)));
        }
        if (languages.isEmpty()) {
            languages.add(localeToWikiLanguageCode(Locale.getDefault()));
        }
        return languages;
    }

    /**
     * Takes a Locale (as returned by Android) and returns a wiki code, as used by Wikipedia.
     *
     * @param locale Locale (as returned by Android)
     * @return Wiki code, as used by wikipedia.
     */
    @NonNull public static String localeToWikiLanguageCode(@NonNull Locale locale) {
        // Convert deprecated language codes to modern ones.
        // See https://developer.android.com/reference/java/util/Locale.html
        switch (locale.getLanguage()) {
            case "iw":
                return "he"; // Hebrew
            case "in":
                return "id"; // Indonesian
            case "ji":
                return "yi"; // Yiddish
            case "yue": // Cantonese
                return AppLanguageLookUpTable.CHINESE_YUE_LANGUAGE_CODE;
            case "zh":
                return chineseLanguageCodeToWikiLanguageCode(locale);
            default:
                return locale.getLanguage();
        }
    }

    @NonNull private static String chineseLanguageCodeToWikiLanguageCode(@NonNull Locale locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String script = locale.getScript();
            switch (script) {
                case "Hans": return AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
                case "Hant": return AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE;
                default: break;
            }
        }

        // Guess based on country. If the guess is incorrect, the user must explicitly choose the
        // dialect in the app settings.
        return isTraditionalChinesePredominantInCountry(locale.getCountry())
                ? AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE
                : AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
    }

    private static boolean isTraditionalChinesePredominantInCountry(@Nullable String country) {
        return TRADITIONAL_CHINESE_COUNTRY_CODES.contains(country);
    }

    private LanguageUtil() { }
}
