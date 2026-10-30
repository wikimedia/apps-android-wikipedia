package org.wikipedia.readinglist.recommended

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RecommendedReadingListNotificationManagerTest {

    @Test
    fun testNextSendTimeBeforeSendDay() {
        val nextSendTime = RecommendedReadingListNotificationManager.getNotificationsExperimentNextSendTime(at("2026-10-28T09:00"), fromFiredAlarm = false)
        assertEquals(LocalDateTime.parse("2026-11-02T18:00"), nextSendTime.toLocalDateTime())
    }

    @Test
    fun testNextSendTimeOnSendDayBeforeSendTime() {
        val nextSendTime = RecommendedReadingListNotificationManager.getNotificationsExperimentNextSendTime(at("2026-10-26T09:00"), fromFiredAlarm = false)
        assertEquals(LocalDateTime.parse("2026-10-26T18:00"), nextSendTime.toLocalDateTime())
    }

    @Test
    fun testNextSendTimeAfterSendTime() {
        val nextSendTime = RecommendedReadingListNotificationManager.getNotificationsExperimentNextSendTime(at("2026-10-26T18:00"), fromFiredAlarm = false)
        assertEquals(LocalDateTime.parse("2026-11-02T18:00"), nextSendTime.toLocalDateTime())
    }

    @Test
    fun testNextSendTimeStaysAtLocalSendTimeAcrossDstChange() {
        // Clocks fall back on Sunday, Nov 1 2026 in New York, so 6 days 23 hours of local time are 7 days of elapsed time.
        val now = at("2026-10-26T19:00")
        val nextSendTime = RecommendedReadingListNotificationManager.getNotificationsExperimentNextSendTime(now, fromFiredAlarm = false)
        assertEquals(LocalDateTime.parse("2026-11-02T18:00"), nextSendTime.toLocalDateTime())
        assertEquals(now.toInstant().plus(Duration.ofDays(7)), nextSendTime.toInstant())
    }

    @Test
    fun testNextSendTimeSkipsSameDayWhenAlarmFiredEarly() {
        // e.g. the user moved to a time zone where the scheduled moment falls before the local send time.
        val nextSendTime = RecommendedReadingListNotificationManager.getNotificationsExperimentNextSendTime(at("2026-10-26T15:00"), fromFiredAlarm = true)
        assertEquals(LocalDateTime.parse("2026-11-02T18:00"), nextSendTime.toLocalDateTime())
    }

    private fun at(localDateTime: String): ZonedDateTime {
        return ZonedDateTime.of(LocalDateTime.parse(localDateTime), ZoneId.of("America/New_York"))
    }
}
