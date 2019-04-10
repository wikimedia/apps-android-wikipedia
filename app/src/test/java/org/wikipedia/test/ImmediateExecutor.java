package org.wikipedia.test;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;

public class ImmediateExecutor implements Executor {
    @Override
    public void execute(@NonNull Runnable runnable) {
        runnable.run();
    }
}
