package org.wikipedia.feed.topread

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertEquals(LocalDate.of(2016, 6, 1), subject.localDate)
        assertNotNull(subject.articles)
        assertEquals(40, subject.articles.size)
    }

    companion object {
        @Throws(Throwable::class)
        fun unmarshal(filename: String): TopRead {
            val json = TestFileUtil.readRawFile(filename)
            return JsonUtil.decodeFromString(json)!!
        }
    }
}
