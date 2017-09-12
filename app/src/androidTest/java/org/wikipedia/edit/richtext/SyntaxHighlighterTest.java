package org.wikipedia.edit.richtext;

import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.widget.EditText;

import org.junit.Test;
import org.wikipedia.R;
import org.wikipedia.testlib.TestLatch;

import java.util.List;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

public class SyntaxHighlighterTest {
    @Test
    public void testSyntaxHighlight() {
        final String testStr = "foo {{template1}} bar {{template2}} baz";
        final int span1Start = 4;
        final int span1End = 17;
        final int span2Start = 22;
        final int span2End = 35;

        Callback callback = new Callback();
        EditText editText = new EditText(getContext());
        new SyntaxHighlighter(getContext(), editText, callback);
        editText.setText(testStr);

        List<SpanExtents> result = callback.await();
        assertThat(result.size(), is(2));
        assertThat(result.get(0), instanceOf(ColorSpanEx.class));
        assertThat(result.get(0).getStart(), is(span1Start));
        assertThat(result.get(0).getEnd(), is(span1End));
        assertThat(result.get(1), instanceOf(ColorSpanEx.class));
        assertThat(result.get(1).getStart(), is(span2Start));
        assertThat(result.get(1).getEnd(), is(span2End));
    }

    @NonNull
    private ContextThemeWrapper getContext() {
        return new ContextThemeWrapper(getTargetContext(), R.style.ThemeLight);
    }

    private static class Callback implements SyntaxHighlighter.OnSyntaxHighlightListener {
        private final TestLatch latch = new TestLatch();
        private List<SpanExtents> result;

        @Override
        public void syntaxHighlightResults(List<SpanExtents> spanExtents) {
            result = spanExtents;
            latch.countDown();
        }

        public List<SpanExtents> await() {
            latch.await();
            return result;
        }
    }
}
