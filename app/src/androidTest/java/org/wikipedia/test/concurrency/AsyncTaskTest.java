package org.wikipedia.test.concurrency;

import android.test.ActivityUnitTestCase;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.test.TestDummyActivity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncTaskTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 1000;

    public AsyncTaskTest() {
        super(TestDummyActivity.class);
    }

    public void testFinishHandling() throws Throwable {
        final CountDownLatch onFinishLatch = new CountDownLatch(1);
        final Integer returned = 42;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SaneAsyncTask<Integer>() {
                    @Override
                    public void onFinish(Integer result) {
                        assertEquals(returned, result);
                        onFinishLatch.countDown();
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        assertTrue("Exception called despite success", false);
                    }

                    @Override
                    public Integer performTask() throws Throwable {
                        return returned;
                    }
                }.execute();
            }
        });
        assertTrue(onFinishLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testExceptionHandling() throws Throwable {
        final CountDownLatch exceptionLatch = new CountDownLatch(1);
        final Throwable thrown = new Exception();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new SaneAsyncTask<Void>() {
                    @Override
                    public void onFinish(Void result) {
                        assertTrue("onFinish called despite exception", false);
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        assertSame(caught, thrown);
                        exceptionLatch.countDown();
                    }

                    @Override
                    public Void performTask() throws Throwable {
                        throw thrown;
                    }
                }.execute();
            }
        });
        assertTrue(exceptionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testAppropriateThreadFinish() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Thread callingThread = Thread.currentThread();
                new SaneAsyncTask<Thread>() {
                    @Override
                    public void onBeforeExecute() {
                        assertSame(callingThread, Thread.currentThread());
                    }

                    @Override
                    public void onFinish(Thread result) {
                        assertNotSame(result, Thread.currentThread());
                        assertSame(Thread.currentThread(), callingThread);
                        completionLatch.countDown();
                    }

                    @Override
                    public Thread performTask() throws Throwable {
                        assertNotSame(callingThread, Thread.currentThread());
                        return Thread.currentThread();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testAppropriateThreadException() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        final Throwable thrown = new Exception();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Thread callingThread = Thread.currentThread();
                new SaneAsyncTask<Thread>() {
                    @Override
                    public void onBeforeExecute() {
                        assertSame(callingThread, Thread.currentThread());
                    }

                    @Override
                    public void onFinish(Thread result) {
                        assertTrue("onFinish called even when there is an exception", false);
                    }

                    @Override
                    public void onCatch(Throwable caught) {
                        assertSame(callingThread, Thread.currentThread());
                        completionLatch.countDown();
                    }

                    @Override
                    public Thread performTask() throws Throwable {
                        assertNotSame(callingThread, Thread.currentThread());
                        throw thrown;
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
