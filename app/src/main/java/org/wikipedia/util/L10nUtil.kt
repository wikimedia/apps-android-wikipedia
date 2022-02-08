package org.wikipedia.util

import android.content.res.Configuration
import android.content.res.Resources
import android.text.TextUtils
import android.util.SparseArray
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import org.wikipedia.WikipediaApp
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
import java.util.*

object L10nUtil {
    @JvmStatic
    val isDeviceRTL: Boolean
        get() = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL

    private val currentConfiguration: Configuration
        get() = Configuration(WikipediaApp.getInstance().resources.configuration)

    @JvmStatic
    fun isLangRTL(lang: String): Boolean {
        return TextUtils.getLayoutDirectionFromLocale(Locale(lang)) == View.LAYOUT_DIRECTION_RTL
    }

    @JvmStatic
    fun setConditionalTextDirection(view: View, lang: String) {
        view.textDirection = if (isLangRTL(lang)) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
    }

    @JvmStatic
    fun setConditionalLayoutDirection(view: View, lang: String) {
        view.layoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale(lang))
    }

    @JvmStatic
    fun getStringForArticleLanguage(languageCode: String, resId: Int): String {
        return getStringsForLocale(Locale(languageCode), intArrayOf(resId))[resId]
    }

    @JvmStatic
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
        if (systemLocale.language == targetLocale.language) {
            val localizedStrings = SparseArray<String>()
            strings.forEach {
                localizedStrings.put(it, WikipediaApp.getInstance().getString(it))
            }
            return localizedStrings
        }
        setDesiredLocale(config, targetLocale)
        val localizedStrings = getTargetStrings(strings, config)
        config.setLocale(systemLocale)
        // reset to current configuration
        WikipediaApp.getInstance().createConfigurationContext(config)
        return localizedStrings
    }

    // To be used only for plural strings and strings requiring arguments
    fun getResourcesForWikiLang(languageCode: String): Resources? {
        val config = currentConfiguration
        val targetLocale = Locale(languageCode)
        val systemLocale = ConfigurationCompat.getLocales(config)[0]
        if (systemLocale.language == targetLocale.language) {
            return null
        }
        setDesiredLocale(config, targetLocale)
        val targetResources = WikipediaApp.getInstance().createConfigurationContext(config).resources
        config.setLocale(systemLocale)
        // reset to current configuration
        WikipediaApp.getInstance().createConfigurationContext(config)
        return targetResources
    }

    private fun getTargetStrings(@StringRes strings: IntArray, altConfig: Configuration): SparseArray<String> {
        val localizedStrings = SparseArray<String>()
        val targetResources = WikipediaApp.getInstance().createConfigurationContext(altConfig).resources
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

    @JvmStatic
    fun getDesiredLanguageCode(langCode: String): String {
        return when (langCode) {
            TRADITIONAL_CHINESE_LANGUAGE_CODE, CHINESE_TW_LANGUAGE_CODE, CHINESE_HK_LANGUAGE_CODE,
            CHINESE_MO_LANGUAGE_CODE -> TRADITIONAL_CHINESE_LANGUAGE_CODE
            SIMPLIFIED_CHINESE_LANGUAGE_CODE, CHINESE_CN_LANGUAGE_CODE, CHINESE_SG_LANGUAGE_CODE,
            CHINESE_MY_LANGUAGE_CODE -> SIMPLIFIED_CHINESE_LANGUAGE_CODE
            else -> langCode
        }
    }

    @JvmStatic
    fun setDesiredLocale(config: Configuration, desiredLocale: Locale) {
        // when loads API in chinese variant, we can get zh-hant, zh-hans and zh
        // but if we want to display chinese correctly based on the article itself, we have to
        // detect the variant from the API responses; otherwise, we will only get english texts.
        // And this might only happen in Chinese variant
        if (desiredLocale.language == CHINESE_LANGUAGE_CODE) {
            // create a new Locale object to manage only "zh" language code based on its app language
            // code. e.g.: search "HK" article in "zh-hant" or "zh-hans" will get "zh" language code
            config.setLocale(getDesiredLocale(Locale(WikipediaApp.getInstance().language().appLanguageCode)))
        } else {
            config.setLocale(getDesiredLocale(desiredLocale))
        }
    }
}
