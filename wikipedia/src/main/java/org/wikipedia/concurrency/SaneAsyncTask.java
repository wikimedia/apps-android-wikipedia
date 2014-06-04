package org.wikipedia.concurrency;

import java.util.concurrent.Executor;

public abstract class SaneAsyncTask<T> {
    public static final int SINGLE_THREAD = 1;
    public static final int LOW_CONCURRENCY = 2;
    public static final int HIGH_CONCURRENCY = 4;

    private final BackingAsyncTask underlyingTask;

    private final Executor executor;

    /**
     * @param executor The executor on which this Task will run.
     */
    public SaneAsyncTask(Executor executor) {
        this.executor = executor;
        underlyingTask =  new BackingAsyncTask();
    }

    /**
     * Creates an executor using the default ExecutorService.
     *
     * @param concurrencyLevel Number of threads to use at max for this thread pool.
     */
    public SaneAsyncTask(int concurrencyLevel) {
        this.executor = ExecutorService.getSingleton().getExecutor(getClass(), concurrencyLevel);
        this.underlyingTask = new BackingAsyncTask();
    }

    /**
     * Called before the background task is executed.
     * <p/>
     * Called on the UI Thread.
     */
    public void onBeforeExecute() {

    }

    /**
     * Called when the background operation finishes successfully.
     * <p/>
     * Called on the UI Thread.
     *
     * @param result The result of the background operation.
     */
    public void onFinish(T result) {

    }

    /**
     * Called when an exception is thrown in the background process.
     * <p/>
     * Called on the UI Thread.
     *
     * Default implementation just throws it as a RuntimeException, so exceptions are never swallowed.
     *
     * @param caught The exception that was thrown.
     */
    public void onCatch(Throwable caught) {
        throw new RuntimeException(caught);
    }


    /**
     * Called to perform the actual work in the background.
     *
     * Called on a background thread.
     * @return The result of the operation that needed to be run in background.
     */
    public abstract T performTask() throws Throwable;

    /**
     * Start performing the task on the executor specified.
     */
    public void execute() {
        underlyingTask.executeOnExecutor(executor);
    }

    /**
     * Cancel the underlying operation.
     */
    public void cancel() {
        underlyingTask.cancel(true);
    }

    /**
     * Private AsyncTask that actually performs the operations.
     */
    private class BackingAsyncTask extends AsyncTask<Void, Void, T> {
        private Throwable thrown;

        @Override
        protected T doInBackground(Void... voids) {
            try {
                return performTask();
            } catch (Throwable t) {
                thrown = t;
                return null;
            }
        }

        @Override
        protected void onPostExecute(T result) {
            super.onPostExecute(result);
            if (thrown != null) {
                onCatch(thrown);
            } else {
                onFinish(result);
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onBeforeExecute();
        }
    }
}
