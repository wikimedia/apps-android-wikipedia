package org.wikipedia.zero;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
                new ZeroConfig("Overstay your stay!", "#FFFFFF", "#00FFFF", "", "", "", ""));
    }

    private void testOnFinish(String responseBody,
                              @Nullable ZeroConfig expected) throws Exception {
        TestLatch latch = new TestLatch();
        Subject subject = new Subject(latch, expected);

        server.enqueue(responseBody);

        subject.execute();

        server.takeRequest();
        latch.await();
    }

    private class Subject extends WikipediaZeroTask {
        @NonNull
        private final TestLatch latch;

        @Nullable
        private final ZeroConfig expected;

        Subject(@NonNull TestLatch latch, @Nullable ZeroConfig expected) {
            super(new TestApi(server), "userAgent");
            this.latch = latch;
            this.expected = expected;
        }

        @Override
        public void onFinish(ZeroConfig result) {
            super.onFinish(result);
            assertThat(result, is(expected));
            latch.countDown();
        }

        @Override
        public void execute() {
            super.executeOnExecutor(new ImmediateExecutor());
        }
    }
}
