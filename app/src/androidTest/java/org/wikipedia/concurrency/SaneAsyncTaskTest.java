package org.wikipedia.concurrency;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;
import org.wikipedia.WikipediaApp;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

public class SaneAsyncTaskTest {
    @Test public void testFinishHandling() {
        final TestLatch latch = new TestLatch();
        final Integer returned = 42;
        runOnMainSync(() -> new SaneAsyncTask<Integer>() {
            @Override public void onFinish(Integer result) {
                assertThat(returned, is(result));
                latch.countDown();
            }

            @Override public void onCatch(Throwable caught) {
                fail("Exception called despite success");
            }

            @Override public Integer performTask() throws Throwable {
                return returned;
            }
        }.execute());
        latch.await();
    }

    @Test public void testExceptionHandling() {
        final TestLatch latch = new TestLatch();
        final Throwable thrown = new Exception();
        runOnMainSync(() -> new SaneAsyncTask<Void>() {
            @Override public void onFinish(Void result) {
                fail("onFinish called despite exception");
            }

            @Override public void onCatch(Throwable caught) {
                assertThat(caught, is(thrown));
                latch.countDown();
            }

            @Override public Void performTask() throws Throwable {
                throw thrown;
            }
        }.execute());
        latch.await();
    }

    @Test public void testAppropriateThreadFinish() {
        final TestLatch latch = new TestLatch();
        runOnMainSync(() -> new SaneAsyncTask<Void>() {
            @Override public void onBeforeExecute() {
                assertUiThread();
            }

            @Override public void onFinish(Void result) {
                assertUiThread();
                latch.countDown();
            }

            @Override public Void performTask() throws Throwable {
                assertNotUiThread();
                return null;
            }
        }.execute());
        latch.await();
    }

    @Test public void testAppropriateThreadException() {
        final TestLatch latch = new TestLatch();
        runOnMainSync(() -> new SaneAsyncTask<Void>() {
            @Override public void onBeforeExecute() {
                assertUiThread();
            }

            @Override public void onFinish(Void result) {
                fail("onFinish called even when there is an exception");
            }

            @Override public void onCatch(Throwable caught) {
                assertUiThread();
                latch.countDown();
            }

            @Override public Void performTask() throws Throwable {
                assertNotUiThread();
                throw new Exception();
            }
        }.execute());
        latch.await();
    }

    private void assertUiThread() {
        assertThat(Thread.currentThread(), is(uiThread()));
    }

    private void assertNotUiThread() {
        assertThat(Thread.currentThread(), not(uiThread()));
    }

    @NonNull private Thread uiThread() {
        return WikipediaApp.getInstance().getMainLooper().getThread();
    }

    private void runOnMainSync(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
