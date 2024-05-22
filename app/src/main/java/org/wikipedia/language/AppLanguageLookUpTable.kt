package org.wikipedia.language

import android.content.Context
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import java.util.Locale

class AppLanguageLookUpTable(context: Context) {
    private val resources = context.resources

    val codes by lazy {
        getStringList(R.array.preference_language_keys)
    }

    val bcp47codes by lazy {
        val bcpCodes = mutableMapOf<String, String>()
        val bcpList = getStringList(R.array.preference_bcp47_keys)
        for (i in bcpList.indices) {
            bcpCodes[codes[i]] = bcpList[i]
        }
        bcpCodes
    }

    private val canonicalNames by lazy {
        getStringList(R.array.preference_language_canonical_names)
    }

    private val localizedNames by lazy {
        getStringList(R.array.preference_language_local_names)
    }

    private val languagesVariants by lazy {
        getStringList(R.array.preference_language_variants)
            .map { it.split(",") }
            .filter { it.size > 1 }
            .associate { it[0] to ArrayList(it.subList(1, it.size)) }
    }

    fun getCanonicalName(code: String?): String? {
        var name = canonicalNames.getOrNull(indexOfCode(code))
        if (name.isNullOrEmpty() && !code.isNullOrEmpty()) {
            if (code == Locale.CHINESE.language) {
                name = Locale.CHINESE.getDisplayName(Locale.ENGLISH)
            } else if (code == NORWEGIAN_LEGACY_LANGUAGE_CODE) {
                name = canonicalNames.getOrNull(indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE))
            }
        }
        return name
    }

    fun getLocalizedName(code: String?): String? {
        var name = localizedNames.getOrNull(indexOfCode(code))
        if (name.isNullOrEmpty() && !code.isNullOrEmpty()) {
            when (code) {
                Locale.CHINESE.language -> {
                    name = Locale.CHINESE.getDisplayName(Locale.CHINESE)
                }
                NORWEGIAN_LEGACY_LANGUAGE_CODE -> {
                    name = localizedNames.getOrNull(indexOfCode(NORWEGIAN_BOKMAL_LANGUAGE_CODE))
                }
                BELARUSIAN_TARASK_LANGUAGE_CODE -> {
                    name = localizedNames.getOrNull(indexOfCode(BELARUSIAN_LEGACY_LANGUAGE_CODE))
                }
            }
        }
        return name
    }

    fun getLanguageVariants(code: String?): List<String>? {
        return languagesVariants[code]
    }

    fun getDefaultLanguageCodeFromVariant(code: String?): String? {
        return languagesVariants.entries.firstOrNull { (_, value) -> code in value }?.key
    }

    fun getBcp47Code(code: String): String {
        return bcp47codes[code].orEmpty().ifEmpty { code }
    }

    fun isSupportedCode(code: String?): Boolean {
        return code in codes
    }

    fun indexOfCode(code: String?): Int {
        return codes.indexOf(code)
    }

    private fun getStringList(id: Int): List<String> {
        return resources.getStringArray(id).asList()
    }

    companion object {
        private const val TAIWAN_COUNTRY_CODE = "TW"
        private const val HONG_KONG_COUNTRY_CODE = "HK"
        private const val MACAU_COUNTRY_CODE = "MO"
        private const val SINGAPORE_COUNTRY_CODE = "SG"
        private const val MALAYSIA_COUNTRY_CODE = "MY"
        private const val CHINA_COUNTRY_CODE = "CN"
        const val SIMPLIFIED_CHINESE_LANGUAGE_CODE = "zh-hans"
        const val TRADITIONAL_CHINESE_LANGUAGE_CODE = "zh-hant"
        const val CHINESE_CN_LANGUAGE_CODE = "zh-cn"
        const val CHINESE_HK_LANGUAGE_CODE = "zh-hk"
        const val CHINESE_MO_LANGUAGE_CODE = "zh-mo"
        const val CHINESE_MY_LANGUAGE_CODE = "zh-my"
        const val CHINESE_SG_LANGUAGE_CODE = "zh-sg"
        const val CHINESE_TW_LANGUAGE_CODE = "zh-tw"
        const val CHINESE_YUE_LANGUAGE_CODE = "zh-yue"
        const val CHINESE_LANGUAGE_CODE = "zh"
        const val NORWEGIAN_LEGACY_LANGUAGE_CODE = "no"
        const val NORWEGIAN_BOKMAL_LANGUAGE_CODE = "nb"
        const val BELARUSIAN_LEGACY_LANGUAGE_CODE = "be-x-old"
        const val BELARUSIAN_TARASK_LANGUAGE_CODE = "be-tarask"
        const val TEST_LANGUAGE_CODE = "test"
        const val FALLBACK_LANGUAGE_CODE = "en" // Must exist in preference_language_keys.

        fun chineseLocaleToWikiLanguageCode(locale: Locale): String {
            // When build a Locale with a language tag that starts with "zh-", the script is empty.
            // Fall back to Traditional Chinese if the script is not specified.
            if (locale.script == "Hans" || locale.script == "Hant" || locale.toLanguageTag().startsWith("zh-")) {
                when (locale.country) {
                    TAIWAN_COUNTRY_CODE -> return CHINESE_TW_LANGUAGE_CODE
                    HONG_KONG_COUNTRY_CODE -> return CHINESE_HK_LANGUAGE_CODE
                    MACAU_COUNTRY_CODE -> return CHINESE_MO_LANGUAGE_CODE
                    SINGAPORE_COUNTRY_CODE -> return CHINESE_SG_LANGUAGE_CODE
                    MALAYSIA_COUNTRY_CODE -> return CHINESE_MY_LANGUAGE_CODE
                    CHINA_COUNTRY_CODE -> return CHINESE_CN_LANGUAGE_CODE
                }
                return if (locale.script == "Hans") SIMPLIFIED_CHINESE_LANGUAGE_CODE
                else TRADITIONAL_CHINESE_LANGUAGE_CODE
            }
            return TRADITIONAL_CHINESE_LANGUAGE_CODE
        }

        fun hasNonRegionalChineseCodes(): Boolean {
            val list = listOf(TRADITIONAL_CHINESE_LANGUAGE_CODE, SIMPLIFIED_CHINESE_LANGUAGE_CODE)
            return WikipediaApp.instance.languageState.appLanguageCodes.any(list::contains)
        }
    }
}
