package org.wikipedia.util

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class DateUtilTest {
    private val localDateTime = LocalDate.of(2017, 5, 25).atTime(21, 13, 47)

    @Test
    fun testIso8601LocalDateTimeParse() {
        val zonedDateTime = localDateTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault())
        MatcherAssert.assertThat(
            DateUtil.iso8601LocalDateTimeParse("2017-05-25T21:13:47Z"),
            Matchers.`is`(zonedDateTime.toLocalDateTime())
        )
    }

    @Test
    fun testDbLocalDateTimeParse() {
        MatcherAssert.assertThat(DateUtil.dbLocalDateTimeParse("20170525211347"), Matchers.`is`(localDateTime))
    }

    @Test
    fun testDbLocalDateTimeFormat() {
        MatcherAssert.assertThat(DateUtil.dbLocalDateTimeFormat(localDateTime), Matchers.`is`("20170525211347"))
    }

    @Test
    fun testFormatAsLegacyDateString() {
        val instant = Instant.now()
        MatcherAssert.assertThat(DateUtil.formatAsLegacyDateString(instant), Matchers.`is`(Date.from(instant).toString()))
    }
}
