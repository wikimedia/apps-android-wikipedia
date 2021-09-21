package org.wikipedia.feed.featured

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class FeaturedArticleCardTest {
    private lateinit var content: PageSummary

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("featured_2016_11_07.json")
        content = GsonUnmarshaller.unmarshal(PageSummary::class.java, json)
    }

    @Test
    fun testTitleNormalization() {
        val tfaCard = FeaturedArticleCard(content, 0, TEST)
        MatcherAssert.assertThat(tfaCard.title(), Matchers.not(Matchers.containsString("_")))
    }

    companion object {
        private val TEST = forLanguageCode("test")
    }
}
