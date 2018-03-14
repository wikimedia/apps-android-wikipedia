package org.wikipedia.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DateUtilTest {
    private static final String HTTP_DATE_HEADER = "Thu, 25 May 2017 21:13:47 GMT";

    @Test
    public void testGetHttpLastModifiedDate() throws Throwable {
        assertThat(DateUtil.getShortDateString(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), is("May 25, 2017"));
    }

    @Test
    public void testIso8601DateFormat() throws Throwable {
        SimpleDateFormat format = DateUtil.getIso8601DateFormat();
        assertThat(format.format(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), is("2017-05-25T21:13:47Z"));
    }

    @Test
    public void testIso8601LocalDateFormat() throws Throwable {
        SimpleDateFormat format = DateUtil.getIso8601LocalDateFormat();
        format.setTimeZone(TimeZone.getTimeZone("GMT-4:00"));
        assertThat(format.format(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), is("2017-05-25T17:13:47-0400"));
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertThat(format.format(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), is("2017-05-25T21:13:47+0000"));
    }
}
