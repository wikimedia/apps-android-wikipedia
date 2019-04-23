package org.wikipedia.test;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

public final class ImmediateExecutorService extends AbstractExecutorService {
    @Override public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @NonNull @Override public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException();
    }

    @Override public boolean isShutdown() {
        throw new UnsupportedOperationException();
    }

    @Override public boolean isTerminated() {
        throw new UnsupportedOperationException();
    }

    @Override public boolean awaitTermination(long l, @NonNull TimeUnit timeUnit) {
        throw new UnsupportedOperationException();
    }

    @Override public void execute(@NonNull Runnable runnable) {
        runnable.run();
    }
}
