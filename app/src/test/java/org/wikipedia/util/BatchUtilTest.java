package org.wikipedia.util;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class) public class BatchUtilTest {
    private ArrayList<PageTitle> titles;
    private static final int TOTAL = 120;

    @Before public void setUp() throws Throwable {
        titles = new ArrayList<>();
        for (int i = 0; i < TOTAL; i++) {
            titles.add(new PageTitle("foo", WikiSite.forLanguageCode("test")));
        }
    }

    @Test @SuppressWarnings("checkstyle:magicnumber") public void testMwApiBatches() throws Throwable {
        final TestLatch latch = new TestLatch();

        BatchUtil.makeBatches(titles, new BatchUtil.Handler<Integer>() {
            private List<Integer> sizes = new ArrayList<>();
            private int count;

            @Override public void handleBatch(@NonNull List<PageTitle> batchTitles, int total,
                                              BatchUtil.Callback<Integer> cb) {
                sizes.add(batchTitles.size());
                count += batchTitles.size();

                if (count == TOTAL) {
                    assertThat(sizes.get(0), is(50));
                    assertThat(sizes.get(1), is(50));
                    assertThat(sizes.get(2), is(20));
                    latch.countDown();
                }
            }
        }, null);
        latch.await();
    }
}
