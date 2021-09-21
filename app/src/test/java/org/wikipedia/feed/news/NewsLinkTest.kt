package org.wikipedia.feed.news

import com.google.common.reflect.TypeToken
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.dataclient.WikiSite.Companion.forLanguageCode
import org.wikipedia.json.GsonUtil
import org.wikipedia.test.TestFileUtil

@RunWith(RobolectricTestRunner::class)
class NewsLinkTest {
    private lateinit var content: List<NewsItem>

    @Before
    @Throws(Throwable::class)
    fun setUp() {
        val json = TestFileUtil.readRawFile("news_2016_11_07.json")
        val typeToken = object : TypeToken<List<NewsItem>>() {}
        content = GsonUtil.getDefaultGson().fromJson(json, typeToken.type)
    }

    @Test
    fun testTitleNormalization() {
        for (newsItem in content) {
            for (link in newsItem.links) {
                MatcherAssert.assertThat(
                    NewsLinkCard(link!!, TEST).title(),
                    Matchers.not(Matchers.containsString("_"))
                )
            }
        }
    }

    companion object {
        private val TEST = forLanguageCode("test")
    }
}
