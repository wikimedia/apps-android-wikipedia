package org.wikipedia.concurrency;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.wikipedia.test.TestUtil.runOnMainSync;

@SmallTest public class SaneAsyncTaskTest {
    @Test public void testFinishHandling() {
        final TestLatch latch = new TestLatch();
        final Integer returned = 42;
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new SaneAsyncTask<Integer>() {
                    @Override
                    public void onFinish(Integer result) {
                        assertThat(returned, is(result));
                        latch.countDown();
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        fail("Exception called despite success");
                    }

                    @Override
                    public Integer performTask() throws Throwable {
                        return returned;
                    }
                }.execute();
            }
        });
        latch.await();
    }

    @Test public void testExceptionHandling() {
        final TestLatch latch = new TestLatch();
        final Throwable thrown = new Exception();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new SaneAsyncTask<Void>() {
                    @Override
                    public void onFinish(Void result) {
                        fail("onFinish called despite exception");
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        assertThat(caught, is(thrown));
                        latch.countDown();
                    }

                    @Override
                    public Void performTask() throws Throwable {
                        throw thrown;
                    }
                }.execute();
            }
        });
        latch.await();
    }

    @Test public void testAppropriateThreadFinish() throws Throwable {
        final TestLatch latch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final Thread callingThread = Thread.currentThread();
                new SaneAsyncTask<Thread>() {
                    @Override
                    public void onBeforeExecute() {
                        assertThat(callingThread, is(Thread.currentThread()));
                    }

                    @Override
                    public void onFinish(Thread result) {
                        assertThat(result, not(Thread.currentThread()));
                        assertThat(Thread.currentThread(), is(callingThread));
                        latch.countDown();
                    }

                    @Override
                    public Thread performTask() throws Throwable {
                        assertThat(callingThread, not(Thread.currentThread()));
                        return Thread.currentThread();
                    }
                }.execute();
            }
        });
        latch.await();
    }

    @Test public void testAppropriateThreadException() throws Throwable {
        final TestLatch latch = new TestLatch();
        final Throwable thrown = new Exception();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final Thread callingThread = Thread.currentThread();
                new SaneAsyncTask<Thread>() {
                    @Override
                    public void onBeforeExecute() {
                        assertThat(callingThread, is(Thread.currentThread()));
                    }

                    @Override
                    public void onFinish(Thread result) {
                        fail("onFinish called even when there is an exception");
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        assertThat(callingThread, is(Thread.currentThread()));
                        latch.countDown();
                    }

                    @Override
                    public Thread performTask() throws Throwable {
                        assertThat(callingThread, not(Thread.currentThread()));
                        throw thrown;
                    }
                }.execute();
            }
        });
        latch.await();
    }
}