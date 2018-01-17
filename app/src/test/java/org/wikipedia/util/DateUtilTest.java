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
}
