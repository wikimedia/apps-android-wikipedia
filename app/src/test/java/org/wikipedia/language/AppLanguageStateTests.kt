package org.wikipedia.language

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp

@RunWith(RobolectricTestRunner::class)
class AppLanguageStateTests {

    private val languageState get() = WikipediaApp.instance.languageState

    @Test
    fun testInitAppLanguages() {
        Assert.assertEquals("en", languageState.appLanguageCode)
    }

    @Test
    fun testAddAppLanguage() {
        languageState.addAppLanguageCode("ja")
        Assert.assertEquals("en", languageState.appLanguageCode)
        Assert.assertEquals(2, languageState.appLanguageCodes.size.toLong())
        Assert.assertEquals("ja", languageState.appLanguageCodes[1])
    }

    @Test
    fun testAddMultipleAppLanguages() {
        val list = mutableListOf<String>()
        list.add("en")
        list.add("ja")
        list.add("es")
        list.add("zh-hant")
        languageState.setAppLanguageCodes(list)
        Assert.assertTrue(languageState.appLanguageCode == "en" && languageState.appLanguageCodes.size == 4)
    }

    @Test
    fun testRemoveAppLanguages() {
        val list = mutableListOf<String>()
        list.add("en")
        list.add("ja")
        list.add("es")
        list.add("zh-hant")
        languageState.setAppLanguageCodes(list)

        val listToRemove = mutableListOf<String>()
        listToRemove.add("en")
        listToRemove.add("zh-hant")
        languageState.removeAppLanguageCodes(listToRemove)
        Assert.assertEquals("ja", languageState.appLanguageCode)
        Assert.assertEquals(2, languageState.appLanguageCodes.size.toLong())
    }
}
