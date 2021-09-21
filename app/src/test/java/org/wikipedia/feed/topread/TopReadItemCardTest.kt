package org.wikipedia.feed.topread

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class TopReadItemCardTest {
    private lateinit var content: TopRead

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("mostread_2016_11_07.json")
        content = GsonUnmarshaller.unmarshal(TopRead::class.java, json)
    }

    @Test
    fun testTitleNormalization() {
        val topReadItemCards = TopReadListCard.toItems(content.articles, TEST)
        for (topReadItemCard in topReadItemCards) {
            MatcherAssert.assertThat(topReadItemCard.title(), Matchers.not(Matchers.containsString("_")))
        }
    }

    companion object {
        private val TEST = forLanguageCode("test")
    }
}
