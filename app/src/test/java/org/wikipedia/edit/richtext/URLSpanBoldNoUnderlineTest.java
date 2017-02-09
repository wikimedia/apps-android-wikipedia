package org.wikipedia.edit.richtext;

import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.richtext.URLSpanBoldNoUnderline;
import org.wikipedia.test.TestParcelUtil;
import org.wikipedia.test.TestRunner;

@RunWith(TestRunner.class) public class URLSpanBoldNoUnderlineTest {
    @Test public void testCtorParcel() throws Throwable {
        Parcelable subject = new URLSpanBoldNoUnderline("url");
        TestParcelUtil.test(subject);
    }
}
