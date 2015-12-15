package org.wikipedia.concurrency;

import android.os.AsyncTask;

import org.wikipedia.util.log.L;

public abstract class SaneAsyncTask<T> extends AsyncTask<Void, Void, T> {
    private Throwable thrown;

    public abstract T performTask() throws Throwable;

    public void onBeforeExecute() { }

    public void onFinish(T result) { }

    public void onCatch(Throwable caught) { }

    public void execute() {
        super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void cancel() {
        cancel(true);
    }

    @Override
    protected final T doInBackground(Void... voids) {
        try {
            return performTask();
        } catch (Throwable t) {
            L.d(t);
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
            onCatch(thrown);
        } else {
            try {
                onFinish(result);
            } catch (Exception e) {
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
