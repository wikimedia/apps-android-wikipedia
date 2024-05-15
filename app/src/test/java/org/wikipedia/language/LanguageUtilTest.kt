package org.wikipedia.language

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LanguageUtilTest {

    @Test
    fun testLanguageCodeToWikiLanguageCodeForChineseScripts() {
        val list = listOf("zh" to AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                "zh-Hans" to AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                "zh-Hant" to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                "zh-CN" to AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                "zh-TW" to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                "zh-HK" to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                "zh-MO" to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE)

        list.forEach {
            val defaultLocale = Locale.getDefault()
            val locale = Locale.forLanguageTag(it.first)
            Locale.setDefault(locale)
            val wikiLang = LanguageUtil.localeToWikiLanguageCode(Locale.forLanguageTag(it.first))
            Assert.assertEquals(wikiLang, it.second)
            Locale.setDefault(defaultLocale)
        }
    }

    @Test
    fun testLanguageCodeToWikiLanguageCodeForChineseCountries() {
        val list = listOf(Locale.CHINESE to AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                Locale.TAIWAN to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                Locale.CHINA to AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE)
        list.forEach {
            val defaultLocale = Locale.getDefault()
            Locale.setDefault(it.first)
            val wikiLang = LanguageUtil.localeToWikiLanguageCode(it.first)
            Assert.assertEquals(wikiLang, it.second)
            Locale.setDefault(defaultLocale)
        }
    }
}
