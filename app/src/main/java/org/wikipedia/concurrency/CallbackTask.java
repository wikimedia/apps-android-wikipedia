package org.wikipedia.concurrency;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class CallbackTask<T> extends SaneAsyncTask<T> {
    public interface Callback<T> {
        void success(T row);
    }

    public interface Task<T> {
        T execute();
    }

    @NonNull private final Task<T> task;
    @Nullable private final Callback<T> callback;

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
        }
    }
}