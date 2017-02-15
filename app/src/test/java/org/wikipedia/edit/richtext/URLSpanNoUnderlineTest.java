package org.wikipedia.edit.richtext;

import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.richtext.URLSpanNoUnderline;
import org.wikipedia.test.TestParcelUtil;
import org.wikipedia.test.TestRunner;

@RunWith(TestRunner.class) public class URLSpanNoUnderlineTest {
    @Test public void testCtorParcel() throws Throwable {
        Parcelable subject = new URLSpanNoUnderline("url");
        TestParcelUtil.test(subject);
    }
}
