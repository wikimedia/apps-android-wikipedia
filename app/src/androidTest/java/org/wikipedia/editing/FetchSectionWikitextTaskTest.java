package org.wikipedia.editing;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.wikipedia.test.TestUtil.runOnMainSync;

@SmallTest
public class FetchSectionWikitextTaskTest {
    @Test public void testPageFetch() {
        final TestLatch latch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", WikiSite.forLanguageCode("test"));
                new FetchSectionWikitextTask(getInstrumentation().getTargetContext(), title, 2) {
                    @Override
                    public void onFinish(String result) {
                        assertThat(result, notNullValue());
                        assertThat(result, is("=== Section1.2 ===\nThis is a subsection"));
                        latch.countDown();
                    }
                }.execute();
            }
        });
        latch.await();
    }
}