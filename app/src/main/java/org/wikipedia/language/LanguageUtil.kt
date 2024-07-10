package org.wikipedia.language

import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import org.apache.commons.lang3.StringUtils
import org.wikipedia.WikipediaApp
import java.util.Locale

object LanguageUtil {
    private const val MAX_SUGGESTED_LANGUAGES = 8
    private const val HONG_KONG_COUNTRY_CODE = "HK"
    private const val MACAU_COUNTRY_CODE = "MO"
    private val TRADITIONAL_CHINESE_COUNTRY_CODES = listOf(Locale.TAIWAN.country, HONG_KONG_COUNTRY_CODE, MACAU_COUNTRY_CODE)

    private val InputMethodSubtype.localeObject: Locale?
        get() {
            val languageTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) languageTag else ""
            val actualTag = languageTag.ifEmpty {
                // The keyboard reports locale variants with underscores ("en_US") whereas
                // Locale.forLanguageTag() expects dashes ("en-US"), so convert them.
                @Suppress("DEPRECATION")
                locale.replace('_', '-')
            }
            return if (actualTag.isNotEmpty()) Locale.forLanguageTag(actualTag) else null
        }

    val suggestedLanguagesFromSystem: Sequence<String>
        get() {
            // First, look at languages installed on the system itself.
            val systemLocales = sequence {
                val localeList = LocaleListCompat.getDefault()
                for (i in 0 until localeList.size()) {
                    yield(localeList[i]!!)
                }
            }

            // Query the installed keyboard languages lazily.
            val imm = WikipediaApp.instance.getSystemService<InputMethodManager>()!!
            val keyboardLocales = imm.enabledInputMethodList.asSequence()
                .flatMap { imm.getEnabledInputMethodSubtypeList(it, true) }
                .filter { it.mode == "keyboard" }
                .mapNotNull { it.localeObject }
                .flatMap {
                    // A Pinyin keyboard will report itself as zh-CN (simplified), but we want to add
                    // both Simplified and Traditional in that case.
                    if (it == Locale.SIMPLIFIED_CHINESE) listOf(it, Locale.TRADITIONAL_CHINESE) else listOf(it)
                }

            return (systemLocales + keyboardLocales)
                .map { localeToWikiLanguageCode(it) }
                .distinct()
                .filter { it.isNotEmpty() && it != "und" }
                .take(MAX_SUGGESTED_LANGUAGES)
        }

    fun localeToWikiLanguageCode(locale: Locale): String {
        // Convert deprecated language codes to modern ones.
        // See https://developer.android.com/reference/java/util/Locale.html
        return when (locale.language) {
            "iw" -> "he" // Hebrew
            "in" -> "id" // Indonesian
            "ji" -> "yi" // Yiddish
            "yue" -> AppLanguageLookUpTable.CHINESE_YUE_LANGUAGE_CODE
            "zh" -> chineseLanguageCodeToWikiLanguageCode(locale)
            else -> locale.language
        }
    }

    private fun chineseLanguageCodeToWikiLanguageCode(locale: Locale): String {
        when (locale.script) {
            "Hans" -> return AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE
            "Hant" -> return AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE
        }

        // Guess based on country. If the guess is incorrect, the user must explicitly choose the
        // dialect in the app settings.
        return if (isTraditionalChinesePredominantInCountry(locale.country)) AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE
        else AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE
    }

    private fun isTraditionalChinesePredominantInCountry(country: String?): Boolean {
        return TRADITIONAL_CHINESE_COUNTRY_CODES.contains(country)
    }

    val firstSelectedChineseVariant: String
        get() {
            val firstSelectedChineseLangCode =
                WikipediaApp.instance.languageState.appLanguageCodes.firstOrNull {
                    isChineseVariant(it)
                }
            return firstSelectedChineseLangCode.orEmpty()
                .ifEmpty { AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE }
        }

    fun isChineseVariant(langCode: String): Boolean {
        return langCode.startsWith(AppLanguageLookUpTable.CHINESE_LANGUAGE_CODE) &&
                langCode != AppLanguageLookUpTable.CHINESE_YUE_LANGUAGE_CODE
    }

    fun startsWithArticle(text: String, language: String): Boolean {
        val first = text.split(" ".toRegex()).toTypedArray()[0].lowercase(Locale.getDefault()).trim()

        // When adding new languages:
        // # Update the documentation of the message description_starts_with_article
        // # Contact translators to this language to make sure this message is translated.
        return (language == "en" && StringUtils.equalsAny(first, "a", "an", "the") ||
                language == "de" && StringUtils.equalsAny(first, "der", "den", "dem", "des", "das", "die", "den", "ein", "eine", "einer", "einen", "einem", "eines", "keine", "keinen", "keiner") ||
                language == "es" && StringUtils.equalsAny(first, "el", "los", "la", "las", "un", "unos", "una", "unas") ||
                language == "fr" && (StringUtils.equalsAny(first, "le", "la", "les", "un", "une", "des") ||
                first.startsWith("l'")))
    }

    fun convertToUselangIfNeeded(languageCode: String): String {
        return if (languageCode == "test") "uselang" else languageCode
    }

    fun formatLangCodeForButton(languageCode: String): String {
        return languageCode.replace("-", "-\n")
    }
}
