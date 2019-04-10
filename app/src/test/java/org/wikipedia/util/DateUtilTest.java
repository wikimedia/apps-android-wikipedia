package org.wikipedia.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
        assertThat(DateUtil.iso8601DateFormat(DateUtil.getHttpLastModifiedDate(HTTP_DATE_HEADER)), is("2017-05-25T21:13:47Z"));
    }

    @Test
    public void testIso8601Identity() throws Throwable {
        assertThat(DateUtil.iso8601DateFormat(DateUtil.iso8601DateParse("2017-05-25T21:13:47Z")), is("2017-05-25T21:13:47Z"));
    }
}
