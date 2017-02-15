package org.wikipedia.test;

import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

public class ImmediateExecutor implements Executor {
    @Override
    public void execute(@NonNull Runnable runnable) {
        runnable.run();
    }
}
