package org.wikipedia.feed.featured

import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class FeaturedArticleCardTest {
    private lateinit var content: PageSummary

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("featured_2016_11_07.json")
        content = JsonUtil.decodeFromString(json)!!
    }

    @Test
    fun testTitleNormalization() {
        val tfaCard = FeaturedArticleCard(content, 0, TEST)
        assertFalse(tfaCard.title().contains("_"))
    }

    companion object {
        private val TEST = WikiSite.forLanguageCode("test")
    }
}
