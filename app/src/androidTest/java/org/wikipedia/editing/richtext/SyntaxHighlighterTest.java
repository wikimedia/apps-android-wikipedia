package org.wikipedia.editing.richtext;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;

import org.junit.Rule;
import org.junit.Test;
import org.wikipedia.page.PageActivity;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class SyntaxHighlighterTest {
    @Rule
    @NonNull
    public final ActivityTestRule<PageActivity> activityRule = new ActivityTestRule<>(PageActivity.class);

    @Test
    public void testSyntaxHighlight() throws Exception {

        final String testStr = "foo {{template1}} bar {{template2}} baz";
        final int span1Start = 4;
        final int span1End = 17;
        final int span2Start = 22;
        final int span2End = 35;

        EditText editText = new EditText(getActivity());
        new SyntaxHighlighter(getActivity(), editText,
                new SyntaxHighlighter.OnSyntaxHighlightListener() {
            @Override
            public void syntaxHighlightResults(List<SpanExtents> spanExtents) {
                assertThat(spanExtents.size(), is(2));
                assertThat(spanExtents.get(0).getClass(), instanceOf(ColorSpanEx.class));
                assertThat(spanExtents.get(0).getStart(), is(span1Start));
                assertThat(spanExtents.get(0).getEnd(), is(span1End));
                assertThat(spanExtents.get(1).getClass(), instanceOf(ColorSpanEx.class));
                assertThat(spanExtents.get(1).getStart(), is(span2Start));
                assertThat(spanExtents.get(1).getEnd(), is(span2End));
            }
        });

        editText.setText(testStr);
    }

    private Activity getActivity() {
        return activityRule.getActivity();
    }
}
