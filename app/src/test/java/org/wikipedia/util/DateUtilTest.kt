package org.wikipedia.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DateUtilTest {
    @Test
    @Throws(Throwable::class)
    fun testIso8601Identity() {
        assertEquals(
            "2017-05-25T21:13:47Z",
            DateUtil.iso8601DateParse("2017-05-25T21:13:47Z").toInstant().toString()
        )
        assertEquals(
            "2017-05-25T21:13:47Z",
            DateUtil.iso8601DateParse("2017-05-25T21:13:47.000Z").toInstant().toString()
        )
    }

    companion object {
        private const val HTTP_DATE_HEADER = "Thu, 25 May 2017 21:13:47 GMT"
    }
}
