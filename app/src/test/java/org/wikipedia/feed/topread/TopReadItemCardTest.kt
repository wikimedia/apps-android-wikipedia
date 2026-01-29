package org.wikipedia.feed.topread

import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class TopReadItemCardTest {
    private lateinit var content: TopRead

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("mostread_2016_11_07.json")
        content = JsonUtil.decodeFromString(json)!!
    }

    @Test
    fun testTitleNormalization() {
        val topReadItemCards = TopReadListCard.toItems(content.articles, TEST)
        for (topReadItemCard in topReadItemCards) {
            assertFalse(topReadItemCard.title().contains("_"))
        }
    }

    companion object {
        private val TEST = WikiSite.forLanguageCode("test")
    }
}
