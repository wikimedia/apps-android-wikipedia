package org.wikipedia.concurrency;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.wikipedia.util.log.L;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SaneAsyncTask<T> extends AsyncTask<Void, Void, T> {
    private Throwable thrown;

    // Reimplement the stock AOSP AsyncTask implementation which may differ on OEM devices.
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int QUEUE_SIZE = 128;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger count = new AtomicInteger(1);
        @Override public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, "AsyncTask #" + count.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> POOL_WORK_QUEUE = new LinkedBlockingQueue<>(QUEUE_SIZE);

    private static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, POOL_WORK_QUEUE, THREAD_FACTORY);

    public abstract T performTask() throws Throwable;

    public void onBeforeExecute() { }

    public void onFinish(T result) { }

    public void onCatch(Throwable caught) { }

    public void execute() {
        super.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    public void cancel() {
        cancel(true);
    }

    @Override
    protected final T doInBackground(Void... voids) {
        try {
            return performTask();
        } catch (Throwable t) {
            thrown = t;
            return null;
        }
    }

    @Override
    protected final void onPostExecute(T result) {
        super.onPostExecute(result);
        if (isCancelled()) {
            return;
        }
        if (thrown != null) {
            L.i(thrown);
            onCatch(thrown);
        } else {
            try {
                onFinish(result);
            } catch (Exception e) {
                L.i(e);
                onCatch(e);
            }
        }
    }

    @Override
    protected final void onPreExecute() {
        super.onPreExecute();
        onBeforeExecute();
    }
}
