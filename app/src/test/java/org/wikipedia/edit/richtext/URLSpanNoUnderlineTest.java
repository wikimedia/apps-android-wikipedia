package org.wikipedia.edit.richtext;

import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.richtext.URLSpanNoUnderline;
import org.wikipedia.test.TestParcelUtil;

@RunWith(RobolectricTestRunner.class) public class URLSpanNoUnderlineTest {
    @Test public void testCtorParcel() throws Throwable {
        Parcelable subject = new URLSpanNoUnderline("url");
        TestParcelUtil.test(subject);
    }
}
