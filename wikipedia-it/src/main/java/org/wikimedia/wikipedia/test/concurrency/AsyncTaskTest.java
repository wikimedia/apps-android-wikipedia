package org.wikimedia.wikipedia.test.concurrency;

import android.test.AndroidTestCase;
import com.sun.corba.se.spi.orbutil.threadpool.ThreadPool;
import org.wikimedia.wikipedia.concurrency.ExceptionHandlingAsyncTask;

import java.util.concurrent.*;

public class AsyncTaskTest extends AndroidTestCase {
    public static final int TASK_COMPLETION_TIMEOUT = 100;
    private Executor executor;
    private Executor getDefaultExecutor() {
        if (executor == null) {
            executor = new ScheduledThreadPoolExecutor(1);
        }
        return executor;
    }

    public void testFinishHandling() throws Exception {
        final CountDownLatch onFinishLatch = new CountDownLatch(1);
        final Integer returned = 42;
        new ExceptionHandlingAsyncTask<Integer>(getDefaultExecutor()) {
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
        assertTrue(onFinishLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    public void testExceptionHandling() throws Exception {
        final CountDownLatch exceptionLatch = new CountDownLatch(1);
        final Throwable thrown = new Exception();
        new ExceptionHandlingAsyncTask<Void>(getDefaultExecutor()) {
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
        assertTrue(exceptionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
