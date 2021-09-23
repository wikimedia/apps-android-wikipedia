package org.wikipedia.feed.topread

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.test.TestFileUtil
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
class TopReadTest {
    @Test
    @Throws(Throwable::class)
    fun testUnmarshalManyArticles() {
        val subject = unmarshal("most_read.json")
        MatcherAssert.assertThat(subject.date, Matchers.`is`(date("2016-06-01Z")))
        MatcherAssert.assertThat(subject.articles, Matchers.notNullValue())
        MatcherAssert.assertThat(subject.articles.size, Matchers.`is`(40))
    }

    @Throws(Throwable::class)
    private fun date(str: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'Z'", Locale.ROOT)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(str)!!
    }

    companion object {
        @Throws(Throwable::class)
        fun unmarshal(filename: String): TopRead {
            val json = TestFileUtil.readRawFile(filename)
            return GsonUnmarshaller.unmarshal(TopRead::class.java, json)
        }
    }
}
