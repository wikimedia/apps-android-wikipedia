package org.wikipedia.language

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LanguageUtilTest {

    @Test
    fun testLanguageCodeToWikiLanguageCodeForChineseLanguageTags() {
        val list = listOf("zh" to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                "zh-Hans" to AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE,
                "zh-Hant" to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
                "zh-CN" to AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE,
                "zh-TW" to AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE,
                "zh-HK" to AppLanguageLookUpTable.CHINESE_HK_LANGUAGE_CODE,
                "zh-MO" to AppLanguageLookUpTable.CHINESE_MO_LANGUAGE_CODE)

        list.forEach {
            val locale = Locale.forLanguageTag(it.first)
            val wikiLang = LanguageUtil.localeToWikiLanguageCode(locale)
            Assert.assertEquals(wikiLang, it.second)
        }
    }

    @Test
    fun testLanguageCodeToWikiLanguageCodeForChineseLocales() {
        // Locale.TRADITIONAL_CHINESE will output `zh_TW` with tag `zh-TW`.
        // Locale.SIMPLIFIED_CHINESE will output `zh_CN` with tag `zh-CN`.
        val list = listOf(Locale.CHINESE to AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE,
            Locale.TAIWAN to AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE,
            Locale.CHINA to AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE,
            Locale.TRADITIONAL_CHINESE to AppLanguageLookUpTable.CHINESE_TW_LANGUAGE_CODE,
            Locale.SIMPLIFIED_CHINESE to AppLanguageLookUpTable.CHINESE_CN_LANGUAGE_CODE)
        list.forEach {
            val wikiLang = LanguageUtil.localeToWikiLanguageCode(it.first)
            Assert.assertEquals(wikiLang, it.second)
        }
    }

    @Test
    fun testLanguageCodeToWikiLanguageCode() {
        val list = listOf(Locale("iw") to "he",
            Locale("yue") to AppLanguageLookUpTable.CHINESE_YUE_LANGUAGE_CODE)
        list.forEach {
            val wikiLang = LanguageUtil.localeToWikiLanguageCode(it.first)
            Assert.assertEquals(wikiLang, it.second)
        }
    }
}
