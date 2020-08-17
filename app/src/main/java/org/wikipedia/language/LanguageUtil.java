package org.wikipedia.language;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.LocaleListCompat;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        // First, look at languages installed on the system itself.
        LocaleListCompat localeList = LocaleListCompat.getDefault();
        for (int i = 0; i < localeList.size(); i++) {
            String languageCode = localeToWikiLanguageCode(localeList.get(i));
            if (!languages.contains(languageCode)) {
                languages.add(languageCode);
            }
        }
        if (languages.isEmpty()) {
            // Always default to at least one system language in the list.
            languages.add(localeToWikiLanguageCode(Locale.getDefault()));
        }

        // Query the installed keyboard languages, and add them to the list, if they don't exist.
        InputMethodManager imm = (InputMethodManager) WikipediaApp.getInstance().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            List<InputMethodInfo> ims = imm.getEnabledInputMethodList();
            if (ims == null) {
                ims = Collections.emptyList();
            }
            List<String> langTagList = new ArrayList<>();
            for (InputMethodInfo method : ims) {
                List<InputMethodSubtype> submethods = imm.getEnabledInputMethodSubtypeList(method, true);
                if (submethods == null) {
                    submethods = Collections.emptyList();
                }
                for (InputMethodSubtype submethod : submethods) {
                    if (submethod.getMode().equals("keyboard")) {
                        String langTag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !TextUtils.isEmpty(submethod.getLanguageTag())
                                ? submethod.getLanguageTag() : submethod.getLocale();
                        if (TextUtils.isEmpty(langTag)) {
                            continue;
                        }
                        if (langTag.contains("_")) {
                            // The keyboard reports locale variants with underscores ("en_US") whereas
                            // Locale.forLanguageTag() expects dashes ("en-US"), so convert them.
                            langTag = langTag.replace('_', '-');
                        }
                        if (!langTagList.contains(langTag)) {
                            langTagList.add(langTag);
                        }
                        // A Pinyin keyboard will report itself as zh-CN (simplified), but we want to add
                        // both Simplified and Traditional in that case.
                        if (langTag.toLowerCase().equals(AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE)
                                && !langTagList.contains("zh-TW")) {
                            langTagList.add("zh-TW");
                        }
                    }
                }
            }
            if (!langTagList.isEmpty()) {
                localeList = LocaleListCompat.forLanguageTags(StringUtil.listToCsv(langTagList));
                for (int i = 0; i < localeList.size(); i++) {
                    String langCode = localeToWikiLanguageCode(localeList.get(i));
                    if (!TextUtils.isEmpty(langCode) && !languages.contains(langCode) && !langCode.equals("und")) {
                        languages.add(langCode);
                    }
                }
            }
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
        String script = locale.getScript();
        switch (script) {
            case "Hans": return AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
            case "Hant": return AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE;
            default: break;
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

    @NonNull
    public static String getFirstSelectedChineseVariant() {
        String firstSelectedChineseLangCode = null;
        for (String langCode : WikipediaApp.getInstance().language().getAppLanguageCodes()) {
            if (langCode.startsWith(AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE)) {
                firstSelectedChineseLangCode = langCode;
                break;
            }
        }
        return StringUtils.defaultString(firstSelectedChineseLangCode, AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE);
    }

    public static boolean startsWithArticle(@NonNull String text, @NonNull String language) {
        String first = text.split(" ")[0].toLowerCase().trim();

        // When adding new languages:
        // # Update the documentation of the message description_starts_with_article
        // # Contact translators to this language to make sure this message is translated.
        return ((language.equals("en") && StringUtils.equalsAny(first, "a", "an", "the"))
                || (language.equals("de") && StringUtils.equalsAny(first, "der", "den", "dem", "des", "das", "die", "den", "ein", "eine", "einer", "einen", "einem", "eines", "keine", "keinen", "keiner"))
                || (language.equals("es") && StringUtils.equalsAny(first, "el", "los", "la", "las", "un", "unos", "una", "unas"))
                || (language.equals("fr") && (StringUtils.equalsAny(first, "le", "la", "les", "un", "une", "des") || first.startsWith("l'"))));
    }

    private LanguageUtil() { }
}
