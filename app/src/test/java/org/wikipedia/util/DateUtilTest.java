package org.wikipedia.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DateUtilTest {
    private static final String HTTP_DATE_HEADER = "Thu, 25 May 2017 21:13:47 GMT";

    @Test
    public void testGetShortDateString() {
        final ZonedDateTime zonedDateTime = ZonedDateTime.parse(HTTP_DATE_HEADER, DateTimeFormatter.RFC_1123_DATE_TIME);
        assertThat(DateUtil.getShortDateString(zonedDateTime.toLocalDate()), is("May 25, 2017"));
    }
}
