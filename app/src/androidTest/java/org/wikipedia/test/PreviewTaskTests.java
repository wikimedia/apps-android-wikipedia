package org.wikipedia.test;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.editing.EditPreviewTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(AndroidJUnit4.class)
public class PreviewTaskTests {
    @Test
    public void testPreview() throws Throwable {
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", new Site("test.wikipedia.org"));
        long randomTime = System.currentTimeMillis();
        final String wikiText = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + randomTime;
        final String expected = "<div></div><h2><span class=\"mw-headline\" id=\"Section_2\">Section 2</span><span><a href=\"#/editor/1\" title=\"Edit section: Section 2\" data-section=\"1\" class=\"mw-ui-icon mw-ui-icon-element mw-ui-icon-edit-enabled edit-page icon-32px\">Edit</a></span></h2><div>\n<p>Editing section INSERT RANDOM &amp; HERE test at " + randomTime + "</p>\n\n\n\n\n\n</div>";

        String result = Subject.execute(wikiText, title);
        assertThat(result, is(expected));
    }

    private static class Subject extends EditPreviewTask {
        public static String execute(String wikiText, PageTitle title) {
            Subject subject = new Subject(wikiText, title);
            subject.execute();
            return subject.await();
        }

        @NonNull private final TestLatch latch = new TestLatch();
        private String result;

        Subject(String wikiText, PageTitle title) {
            super(getTargetContext(), wikiText, title);
        }

        @Override
        public void onFinish(String result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public String await() {
            latch.await();
            return result;
        }
    }
}