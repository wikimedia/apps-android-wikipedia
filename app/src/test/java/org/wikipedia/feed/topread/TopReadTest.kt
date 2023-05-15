package org.wikipedia.feed.topread

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.json.JsonUtil
import org.wikipedia.test.TestFileUtil
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TopReadTest {
    @Test
    @Throws(Throwable::class)
    fun testUnmarshalManyArticles() {
        val subject = unmarshal("most_read.json")
        MatcherAssert.assertThat(subject.localDate, Matchers.`is`(LocalDate.of(2016, 6, 1)))
        MatcherAssert.assertThat(subject.articles, Matchers.notNullValue())
        MatcherAssert.assertThat(subject.articles.size, Matchers.`is`(40))
    }

    companion object {
        @Throws(Throwable::class)
        fun unmarshal(filename: String): TopRead {
            val json = TestFileUtil.readRawFile(filename)
            return JsonUtil.decodeFromString(json)!!
        }
    }
}
