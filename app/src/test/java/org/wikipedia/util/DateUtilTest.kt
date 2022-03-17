package org.wikipedia.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DateUtilTest {
    @Test
    @Throws(Throwable::class)
    fun testGetHttpLastModifiedDate() {
        MatcherAssert.assertThat(DateUtil.getShortDateString(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), Matchers.`is`("May 25, 2017"))
    }

    @Test
    @Throws(Throwable::class)
    fun testIso8601DateFormat() {
        MatcherAssert.assertThat(DateUtil.iso8601DateFormat(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), Matchers.`is`("2017-05-25T21:13:47Z"))
    }

    @Test
    @Throws(Throwable::class)
    fun testIso8601Identity() {
        MatcherAssert.assertThat(DateUtil.iso8601DateFormat(DateUtil.iso8601DateParse("2017-05-25T21:13:47Z")), Matchers.`is`("2017-05-25T21:13:47Z"))
    }

    companion object {
        private const val HTTP_DATE_HEADER = "Thu, 25 May 2017 21:13:47 GMT"
    }
}
