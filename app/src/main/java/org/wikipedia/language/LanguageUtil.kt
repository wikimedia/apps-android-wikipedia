package org.wikipedia.language

import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.util.StringUtil
import java.util.Locale

object LanguageUtil {

    private const val HONG_KONG_COUNTRY_CODE = "HK"
    private const val MACAU_COUNTRY_CODE = "MO"
    private val TRADITIONAL_CHINESE_COUNTRY_CODES = listOf(Locale.TAIWAN.country, HONG_KONG_COUNTRY_CODE, MACAU_COUNTRY_CODE)

    val availableLanguages: List<String>
        get() {
            val languages = mutableListOf<String>()

            // First, look at languages installed on the system itself.
            var localeList = LocaleListCompat.getDefault()
            for (i in 0 until localeList.size()) {
                localeList[i]?.let {
                    val languageCode = localeToWikiLanguageCode(it)
                    if (!languages.contains(languageCode)) {
                        languages.add(languageCode)
                    }
                }
            }
            if (languages.isEmpty()) {
                // Always default to at least one system language in the list.
                languages.add(localeToWikiLanguageCode(Locale.getDefault()))
            }

            // Query the installed keyboard languages, and add them to the list, if they don't exist.
            val imm = WikipediaApp.instance.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val ims = imm.enabledInputMethodList ?: emptyList()
            val langTagList = mutableListOf<String>()
            for (method in ims) {
                val submethods = imm.getEnabledInputMethodSubtypeList(method, true) ?: emptyList()
                for (submethod in submethods) {
                    if (submethod.mode == "keyboard") {
                        var langTag =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && submethod.languageTag.isNotEmpty()) submethod.languageTag
                            else submethod.locale
                        if (langTag.isEmpty()) {
                            continue
                        }
                        if (langTag.contains("_")) {
                            // The keyboard reports locale variants with underscores ("en_US") whereas
                            // Locale.forLanguageTag() expects dashes ("en-US"), so convert them.
                            langTag = langTag.replace('_', '-')
                        }
                        if (!langTagList.contains(langTag)) {
                            langTagList.add(langTag)
                        }
                        // A Pinyin keyboard will report itself as zh-CN (simplified), but we want to add
                        // both Simplified and Traditional in that case.
                        if (langTag.lowercase(Locale.getDefault()) == AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE &&
                            !langTagList.contains("zh-TW")) {
                            langTagList.add("zh-TW")
                        }
                    }
                }
            }
            if (langTagList.isNotEmpty()) {
                localeList = LocaleListCompat.forLanguageTags(StringUtil.listToCsv(langTagList))
                for (i in 0 until localeList.size()) {
                    localeList[i]?.let {
                        val langCode = localeToWikiLanguageCode(it)
                        if (langCode.isNotEmpty() && !languages.contains(langCode) && langCode != "und") {
                            languages.add(langCode)
                        }
                    }
                }
            }
            return languages
        }

    @JvmStatic
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

    // TODO: This is a temporary fix for T355192
    suspend fun getPageSummary(pageSummary: PageSummary?, wikiSite: WikiSite): PageSummary? {
        if (pageSummary == null) {
            return null
        }
        var newPageSummary = pageSummary
        withContext(Dispatchers.Main) {
            // First, get the correct description from Wikidata directly.
            val wikiDataResponse = async {
                ServiceFactory.get(Constants.wikidataWikiSite)
                    .getWikidataDescription(titles = pageSummary.apiTitle, sites = wikiSite.dbName(), langCode = wikiSite.languageCode)
            }
            // Second, fetch PageSummary endpoint instead of using the one with incorrect language variant (mostly from the feed endpoint).
            val pageSummaryResponse = async {
                ServiceFactory.getRest(wikiSite).getPageSummary(null, pageSummary.apiTitle)
            }

            newPageSummary = pageSummaryResponse.await().apply {
                description = wikiDataResponse.await().first?.getDescription(wikiSite.languageCode) ?: description
            }
        }
        return newPageSummary
    }
}
