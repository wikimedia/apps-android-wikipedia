package org.wikipedia.language

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.WikipediaApp.Companion.instance
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.language.LangLinksViewModel.Companion.addVariantEntriesIfNeeded
import org.wikipedia.page.PageTitle

@RunWith(RobolectricTestRunner::class)
class LangLinksActivityTest {
    @Test
    fun testAddChineseEntriesForTraditionalChinese() {
        val title = PageTitle(null, "洋基體育場 (1923年)", forLanguageCode("zh-hant"))
        val list = baseLanguageEntries

        addVariantEntriesIfNeeded(instance.languageState, title, list)
        compareLists(list, expectedZhHantResults)
    }

    @Test
    fun testAddChineseEntriesForSimplifiedChinese() {
        val title = PageTitle(null, "洋基体育场 (1923年)", forLanguageCode("zh-hans"))
        val list = baseLanguageEntries

        addVariantEntriesIfNeeded(instance.languageState, title, list)
        compareLists(list, expectedZhHansResults)
    }

    @Test
    fun testSkipAddChineseEntries() {
        val title = PageTitle(null, "Yankee Stadium (1923)", forLanguageCode("da"))
        val list = baseLanguageEntriesWithZhVariants

        addVariantEntriesIfNeeded(instance.languageState, title, list)
        compareLists(list, expectedGeneralResults)
    }

    private val baseLanguageEntries: MutableList<PageTitle>
        get() {
            val result: MutableList<PageTitle> = ArrayList()
            result.add(PageTitle("Yankee Stadium (1923)", forLanguageCode("en")))
            result.add(PageTitle("ヤンキー・スタジアム (1923年)", forLanguageCode("ja")))
            result.add(PageTitle("양키 스타디움 (1923년)", forLanguageCode("ko")))
            result.add(PageTitle("Yankee Stadium (1923)", forLanguageCode("sv")))
            return result
        }

    private val baseLanguageEntriesWithZhVariants: MutableList<PageTitle>
        get() {
            val result = baseLanguageEntries
            result.add(PageTitle("洋基体育场 (1923年)", forLanguageCode("zh-hans")))
            result.add(
                PageTitle(
                    "洋基体育场 (1923年)",
                    forLanguageCode("zh-hant")
                )
            ) // TODO: change to correct variant, an API issue
            return result
        }

    private val expectedGeneralResults: List<PageTitle>
        get() {
            val result = baseLanguageEntries
            result.add(PageTitle("洋基体育场 (1923年)", forLanguageCode("zh-hans")))
            result.add(
                PageTitle(
                    "洋基体育场 (1923年)",
                    forLanguageCode("zh-hant")
                )
            ) // TODO: change to correct variant, an API issue
            return result
        }


    private val expectedZhHantResults: List<PageTitle>
        get() {
            val result = baseLanguageEntries
            // this order follows the order in languages_list.xml
            val variants = instance.languageState.getLanguageVariants("zh")
            if (variants != null) {
                for (languageCode in variants) {
                    if (languageCode != "zh-hant") {
                        result.add(PageTitle("洋基體育場 (1923年)", forLanguageCode(languageCode)))
                    }
                }
            }
            return result
        }

    private val expectedZhHansResults: List<PageTitle>
        get() {
            val result = baseLanguageEntries
            // this order follows the order in languages_list.xml
            val variants = instance.languageState.getLanguageVariants("zh")
            if (variants != null) {
                for (languageCode in variants) {
                    if (languageCode != "zh-hans") {
                        result.add(PageTitle("洋基体育场 (1923年)", forLanguageCode(languageCode)))
                    }
                }
            }
            return result
        }

    private fun compareLists(list1: List<PageTitle>, list2: List<PageTitle>) {
        for (i in list1.indices) {
            MatcherAssert.assertThat(
                list1[i].uri, Matchers.`is`(
                    list2[i].uri
                )
            )
            MatcherAssert.assertThat(
                list1[i].displayText, Matchers.`is`(
                    list2[i].displayText
                )
            )
        }
    }
}
