package org.wikipedia.util

import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_CN_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_HK_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_MO_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_MY_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_SG_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.CHINESE_TW_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.SIMPLIFIED_CHINESE_LANGUAGE_CODE
import org.wikipedia.language.AppLanguageLookUpTable.Companion.TRADITIONAL_CHINESE_LANGUAGE_CODE
import org.wikipedia.page.PageTitle
import java.util.Locale

object L10nUtil {
    val isDeviceRTL: Boolean
        get() = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL

    private val currentConfiguration: Configuration
        get() = Configuration(WikipediaApp.instance.resources.configuration)

    fun isLangRTL(lang: String): Boolean {
        return TextUtils.getLayoutDirectionFromLocale(Locale(lang)) == View.LAYOUT_DIRECTION_RTL
    }

    fun setConditionalTextDirection(view: View, lang: String) {
        view.textDirection = if (isLangRTL(lang)) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
    }

    fun setConditionalLayoutDirection(view: View, lang: String) {
        view.layoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale(lang))
    }

    fun getStringForArticleLanguage(languageCode: String, resId: Int): String {
        return getStringsForLocale(Locale(languageCode), intArrayOf(resId))[resId]
    }

    fun getStringForArticleLanguage(title: PageTitle, resId: Int): String {
        return getStringsForLocale(Locale(title.wikiSite.languageCode), intArrayOf(resId))[resId]
    }

    fun getStringsForArticleLanguage(title: PageTitle, resId: IntArray): SparseArray<String> {
        return getStringsForLocale(Locale(title.wikiSite.languageCode), resId)
    }

    private fun getStringsForLocale(targetLocale: Locale,
                                    @StringRes strings: IntArray): SparseArray<String> {
        val config = currentConfiguration
        val systemLocale = ConfigurationCompat.getLocales(config)[0]
        if (systemLocale?.language == targetLocale.language) {
            val localizedStrings = SparseArray<String>()
            strings.forEach {
                localizedStrings.put(it, WikipediaApp.instance.getString(it))
            }
            return localizedStrings
        }
        setDesiredLocale(config, targetLocale)
        val localizedStrings = getTargetStrings(strings, config)
        config.setLocale(systemLocale)
        // reset to current configuration
        WikipediaApp.instance.createConfigurationContext(config)
        return localizedStrings
    }

    // To be used only for plural strings and strings requiring arguments
    fun getResourcesForWikiLang(languageCode: String): Resources? {
        val config = currentConfiguration
        val targetLocale = Locale(languageCode)
        val systemLocale = ConfigurationCompat.getLocales(config)[0]
        if (systemLocale?.language == targetLocale.language) {
            return null
        }
        setDesiredLocale(config, targetLocale)
        val targetResources = WikipediaApp.instance.createConfigurationContext(config).resources
        config.setLocale(systemLocale)
        // reset to current configuration
        WikipediaApp.instance.createConfigurationContext(config)
        return targetResources
    }

    private fun getTargetStrings(@StringRes strings: IntArray, altConfig: Configuration): SparseArray<String> {
        val localizedStrings = SparseArray<String>()
        val targetResources = WikipediaApp.instance.createConfigurationContext(altConfig).resources
        strings.forEach {
            localizedStrings.put(it, targetResources.getString(it))
        }
        return localizedStrings
    }

    private fun getDesiredLocale(desiredLocale: Locale): Locale {
        // TODO: maybe other language variants also have this issue, we need to add manually. e.g. kk?
        return when (desiredLocale.language) {
            TRADITIONAL_CHINESE_LANGUAGE_CODE, CHINESE_TW_LANGUAGE_CODE, CHINESE_HK_LANGUAGE_CODE,
            CHINESE_MO_LANGUAGE_CODE -> Locale.TRADITIONAL_CHINESE
            SIMPLIFIED_CHINESE_LANGUAGE_CODE, CHINESE_CN_LANGUAGE_CODE, CHINESE_SG_LANGUAGE_CODE,
            CHINESE_MY_LANGUAGE_CODE -> Locale.SIMPLIFIED_CHINESE
            else -> desiredLocale
        }
    }

    fun getDesiredLanguageCode(langCode: String): String {
        return when (langCode) {
            TRADITIONAL_CHINESE_LANGUAGE_CODE, CHINESE_TW_LANGUAGE_CODE, CHINESE_HK_LANGUAGE_CODE,
            CHINESE_MO_LANGUAGE_CODE -> TRADITIONAL_CHINESE_LANGUAGE_CODE
            SIMPLIFIED_CHINESE_LANGUAGE_CODE, CHINESE_CN_LANGUAGE_CODE, CHINESE_SG_LANGUAGE_CODE,
            CHINESE_MY_LANGUAGE_CODE -> SIMPLIFIED_CHINESE_LANGUAGE_CODE
            else -> langCode
        }
    }

    private fun setDesiredLocale(config: Configuration, desiredLocale: Locale) {
        // when loads API in chinese variant, we can get zh-hant, zh-hans and zh
        // but if we want to display chinese correctly based on the article itself, we have to
        // detect the variant from the API responses; otherwise, we will only get english texts.
        // And this might only happen in Chinese variant
        if (desiredLocale.language == CHINESE_LANGUAGE_CODE) {
            // create a new Locale object to manage only "zh" language code based on its app language
            // code. e.g.: search "HK" article in "zh-hant" or "zh-hans" will get "zh" language code
            config.setLocale(getDesiredLocale(Locale(WikipediaApp.instance.languageState.appLanguageCode)))
        } else {
            config.setLocale(getDesiredLocale(desiredLocale))
        }
    }

    suspend fun getPagesForLanguageVariant(list: List<PageSummary>, wikiSite: WikiSite, shouldUpdateExtracts: Boolean = false): List<PageSummary> {
        return withContext(Dispatchers.IO) {
            if (list.isEmpty()) {
                return@withContext emptyList()
            }
            val newList = mutableListOf<PageSummary>()
            val titles = list.joinToString(separator = "|") { it.apiTitle }
            // First, get the correct description from Wikidata directly.
            val wikiDataResponse = async {
                ServiceFactory.get(Constants.wikidataWikiSite)
                    .getWikidataDescription(titles = titles, sites = wikiSite.dbName(), langCode = wikiSite.languageCode)
            }
            // Second, fetch varianttitles from prop=info endpoint.
            val mwQueryResponse = async {
                ServiceFactory.get(wikiSite).getVariantTitlesByTitles(titles)
            }

            // Third, update the extracts from the page/summary endpoint if needed.
            if (shouldUpdateExtracts) {
                list.map { pageSummary ->
                    async {
                        ServiceFactory.getRest(wikiSite).getPageSummary(null, pageSummary.apiTitle)
                    }
                }.awaitAll().forEachIndexed { index, pageSummary ->
                    list[index].extract = pageSummary.extract
                    list[index].extractHtml = pageSummary.extractHtml
                }
            }

            val mwQueryResult = mwQueryResponse.await()
            val wikiDataResult = wikiDataResponse.await()

            list.forEach { pageSummary ->
                // Find the correct display title from the varianttitles map, and insert the new page summary to the list.
                val displayTitle = mwQueryResult.query?.pages?.find { StringUtil.addUnderscores(it.title) == pageSummary.apiTitle }?.varianttitles?.get(wikiSite.languageCode)
                val newPageSummary = pageSummary.apply {
                    val newDisplayTitle = displayTitle ?: pageSummary.displayTitle
                    this.titles = PageSummary.Titles(
                        canonical = pageSummary.apiTitle,
                        display = newDisplayTitle
                    )
                    this.description = wikiDataResult.entities.values.firstOrNull {
                        it.getLabels()[wikiSite.languageCode]?.value == newDisplayTitle
                    }?.getDescriptions()?.get(wikiSite.languageCode)?.value ?: pageSummary.description
                }
                newList.add(newPageSummary)
            }
            newList
        }
    }
}
