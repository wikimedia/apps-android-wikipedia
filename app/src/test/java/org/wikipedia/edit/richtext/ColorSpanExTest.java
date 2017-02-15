package org.wikipedia.edit.richtext;

import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestParcelUtil;
import org.wikipedia.test.TestRunner;

@RunWith(TestRunner.class) public class ColorSpanExTest {
    @Test public void testCtorParcel() throws Throwable {
        SyntaxRule rule = new SyntaxRule("startSymbol", "endSymbol", SyntaxRuleStyle.BOLD);
        Parcelable subject = new ColorSpanEx(1, 2, 10, rule);
        TestParcelUtil.test(subject);
    }
}
