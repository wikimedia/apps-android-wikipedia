package org.wikipedia.feed.news

import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class NewsLinkTest {
    private lateinit var content: List<NewsItem>

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("news_2016_11_07.json")
        content = JsonUtil.decodeFromString(json)!!
    }

    @Test
    fun testTitleNormalization() {
        for (newsItem in content) {
            for (link in newsItem.links) {
                assertFalse(NewsLinkCard(link!!, TEST).title().contains("_"))
            }
        }
    }

    companion object {
        private val TEST = WikiSite.forLanguageCode("test")
    }
}
