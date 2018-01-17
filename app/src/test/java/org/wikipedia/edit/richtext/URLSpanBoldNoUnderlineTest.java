package org.wikipedia.edit.richtext;

import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.richtext.URLSpanBoldNoUnderline;
import org.wikipedia.test.TestParcelUtil;

@RunWith(RobolectricTestRunner.class) public class URLSpanBoldNoUnderlineTest {
    @Test public void testCtorParcel() throws Throwable {
        Parcelable subject = new URLSpanBoldNoUnderline("url");
        TestParcelUtil.test(subject);
    }
}
