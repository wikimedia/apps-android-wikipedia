package org.wikipedia.page;

import android.support.annotation.NonNull;

import org.wikipedia.testlib.TestLatch;

public class PageLoadLatchCallback implements PageLoadCallbacks {
    private final TestLatch latch = new TestLatch();

    @Override
    public void onLoadComplete() {
        latch.countDown();
    }

    @Override
    public void onLoadError(@NonNull Throwable e) {
        throw new RuntimeException(e);
    }

    public void await() {
        latch.await();
    }
}