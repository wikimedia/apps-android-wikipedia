package org.wikipedia.zero;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowApplication;
import org.wikipedia.test.ImmediateExecutor;
import org.wikipedia.test.TestApi;
import org.wikipedia.test.TestFileUtil;
import org.wikipedia.test.TestRunner;
import org.wikipedia.test.TestWebServer;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(TestRunner.class)
public class WikipediaZeroTaskTest {
    private TestWebServer server = new TestWebServer();

    @Before
    public void setUp() throws Exception {
        server.setUp();
    }

    @After
    public void tearDown() throws Exception {
        server.tearDown();
    }

    @Test
    public void testOnFinishIneligible() throws Exception {
        testOnFinish("{}", null);
    }

    @Test
    public void testOnFinishEligible() throws Exception {
        testOnFinish(TestFileUtil.readRawFile("wikipedia_zero_task_test_eligible.json"),
                new ZeroMessage("Overstay your stay!", "#FFFFFF", "#00FFFF"));
    }

    private void testOnFinish(String responseBody,
                              @Nullable ZeroMessage expected) throws Exception {
        TestLatch latch = new TestLatch();
        Subject subject = new Subject(latch, expected);

        server.enqueue(responseBody);

        subject.execute();
        ShadowApplication.runBackgroundTasks();

        server.takeRequest();
        latch.await();
    }

    private class Subject extends WikipediaZeroTask {
        @NonNull
        private final TestLatch latch;

        @Nullable
        private final ZeroMessage expected;

        public Subject(@NonNull TestLatch latch, @Nullable ZeroMessage expected) {
            super(new ImmediateExecutor(), new TestApi(server), "userAgent");
            this.latch = latch;
            this.expected = expected;
        }

        @Override
        public void onFinish(ZeroMessage result) {
            super.onFinish(result);
            assertThat(result, is(expected));
            latch.countDown();
        }
    }
}
