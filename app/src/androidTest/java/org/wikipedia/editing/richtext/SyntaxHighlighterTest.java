package org.wikipedia.editing.richtext;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.EditText;

import org.wikipedia.page.PageActivity;

import java.util.List;

public class SyntaxHighlighterTest extends ActivityInstrumentationTestCase2<PageActivity> {
    private PageActivity activity;

    public SyntaxHighlighterTest() {
        super(PageActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    public void testSyntaxHighlight() throws Exception {

        final String testStr = "foo {{template1}} bar {{template2}} baz";
        final int span1Start = 4;
        final int span1End = 17;
        final int span2Start = 22;
        final int span2End = 35;

        EditText editText = new EditText(activity);
        new SyntaxHighlighter(activity, editText,
                new SyntaxHighlighter.OnSyntaxHighlightListener() {
            @Override
            public void syntaxHighlightResults(List<SpanExtents> spanExtents) {
                assertEquals(spanExtents.size(), 2);
                assertEquals(spanExtents.get(0).getClass(), ColorSpanEx.class);
                assertEquals(spanExtents.get(0).getStart(), span1Start);
                assertEquals(spanExtents.get(0).getEnd(), span1End);
                assertEquals(spanExtents.get(1).getClass(), ColorSpanEx.class);
                assertEquals(spanExtents.get(1).getStart(), span2Start);
                assertEquals(spanExtents.get(1).getEnd(), span2End);
            }
        });

        editText.setText(testStr);
    }
}
