package org.wikipedia.page;

import org.wikipedia.testlib.TestLatch;

public class PageLoadLatchCallback implements PageLoadCallbacks {
    private final TestLatch latch = new TestLatch();

    @Override
    public void onLoadComplete() {
        latch.countDown();
    }

    public void await() {
        latch.await();
    }
}