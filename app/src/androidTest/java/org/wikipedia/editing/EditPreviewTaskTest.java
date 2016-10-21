package org.wikipedia.editing;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EditPreviewTaskTest {
    @Test
    public void testPreview() throws Throwable {
        final PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", WikiSite.forLanguageCode("test"));
        long randomTime = System.currentTimeMillis();
        final String wikiText = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at " + randomTime;

        String result = Subject.execute(wikiText, title);
        assertThat(result, containsString(String.valueOf(randomTime)));
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
            super(WikipediaApp.getInstance(), wikiText, title);
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
