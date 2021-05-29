package org.wikipedia.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DateUtilTest {
    private static final String HTTP_DATE_HEADER = "Thu, 25 May 2017 21:13:47 GMT";

    @SuppressWarnings("checkstyle:magicnumber")
    private static final LocalDate LOCAL_DATE = LocalDate.of(2010, 1, 1);

    @SuppressWarnings("checkstyle:magicnumber")
    private static final LocalTime LOCAL_TIME = LocalTime.of(2, 3, 4);

    private static final LocalDateTime LOCAL_DATE_TIME = LOCAL_DATE.atTime(LOCAL_TIME);

    @Test
    public void testGetShortDateString() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(HTTP_DATE_HEADER, DateTimeFormatter.RFC_1123_DATE_TIME);
        assertThat(DateUtil.getShortDateString(zonedDateTime.toLocalDate()), is("May 25, 2017"));
    }

    @Test
    public void testIso8601LocalDateFormat() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(HTTP_DATE_HEADER, DateTimeFormatter.RFC_1123_DATE_TIME);
        assertThat(DateUtil.iso8601LocalDateFormat(zonedDateTime), is("2017-05-25T21:13:47+0000"));
    }

    @Test
    public void testDbDateTimeFormat() {
        assertThat(DateUtil.dbDateTimeFormat(LOCAL_DATE_TIME), is("20100101020304"));
    }

    @Test
    public void testDbDateTimeParse() {
        assertThat(DateUtil.dbDateTimeParse("20100101020304"), is(LOCAL_DATE_TIME));
    }

    @Test
    public void testGetFeedCardDateString() {
        assertThat(DateUtil.getFeedCardDateString(LOCAL_DATE), is("Jan 1, 2010"));
    }

    @Test
    public void testGetFeedCardShortDateString() {
        assertThat(DateUtil.getFeedCardShortDateString(LOCAL_DATE), is("Jan 1"));
    }

    @Test
    public void testGetMDYDateString() {
        assertThat(DateUtil.getMDYDateString(LOCAL_DATE), is("01/01/2010"));
    }

    @Test
    public void testGetMonthOnlyDateString() {
        assertThat(DateUtil.getMonthOnlyDateString(LOCAL_DATE), is("January 1"));
    }

    @Test
    public void testGetMonthOnlyWithoutDayDateString() {
        assertThat(DateUtil.getMonthOnlyWithoutDayDateString(LOCAL_DATE), is("January"));
    }

    @Test
    public void testGetTimeString() {
        assertThat(DateUtil.getTimeString(LOCAL_TIME), is("02:03"));
    }

    @Test
    public void testGetDateAndTimeWithPipe() {
        assertThat(DateUtil.getDateAndTimeWithPipe(LOCAL_DATE_TIME), is("Jan 1, 2010 | 02:03"));
    }

    @Test
    public void testLastSyncDateString() {
        assertThat(DateUtil.getLastSyncDateString("2017-05-25T21:13:47Z"),
                is("25 May 2017 21:13"));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testGet24HrFormatTimeOnlyString() {
        assertThat(DateUtil.get24HrFormatTimeOnlyString(LOCAL_TIME), is("02:03"));
        assertThat(DateUtil.get24HrFormatTimeOnlyString(LocalTime.of(23, 1)), is("23:01"));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testYearToStringWithEra() {
        assertThat(DateUtil.yearToStringWithEra(2010), is("2010"));
        assertThat(DateUtil.yearToStringWithEra(-1), is("2 BC"));
    }
}
