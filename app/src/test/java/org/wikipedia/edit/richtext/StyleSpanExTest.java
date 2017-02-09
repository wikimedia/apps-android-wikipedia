package org.wikipedia.edit.richtext;

import android.os.Parcelable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.test.TestParcelUtil;
import org.wikipedia.test.TestRunner;

@RunWith(TestRunner.class) public class StyleSpanExTest {
    @Test public void testCtorParcel() throws Throwable {
        SyntaxRule rule = new SyntaxRule("startSymbol", "endSymbol", SyntaxRuleStyle.BOLD);
        Parcelable subject = new StyleSpanEx(1, 2, rule);
        TestParcelUtil.test(subject);
    }
}
