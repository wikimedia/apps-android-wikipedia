package org.wikipedia.concurrency;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.util.log.L;

public class CallbackTask<T> extends SaneAsyncTask<T> {
    public interface Callback<T> {
        void success(T result);
        void failure(Throwable caught);
    }

    public interface Task<T> {
        T execute() throws Throwable;
    }

    @NonNull private final Task<T> task;
    @Nullable private Callback<T> callback;

    public static <T> void execute(@NonNull Task<T> task) {
        execute(task, null);
    }

    public static <T> void execute(@NonNull Task<T> task, @Nullable Callback<T> callback) {
        new CallbackTask<>(task, callback).execute();
    }

    CallbackTask(@NonNull Task<T> task, @Nullable Callback<T> callback) {
        this.task = task;
        this.callback = callback;
    }

    @Override public T performTask() throws Throwable {
        return task.execute();
    }

    @Override public void onFinish(T result) {
        super.onFinish(result);
        if (callback != null) {
            callback.success(result);
            callback = null;
        }
    }

    @Override public void onCatch(Throwable caught) {
        super.onCatch(caught);
        if (callback != null) {
            callback.failure(caught);
            callback = null;
        }
    }

    public static class DefaultCallback<T> implements Callback<T> {
        @Override public void success(T result) {
        }
        @Override public void failure(Throwable caught) {
            L.e(caught);
        }
    }
}
