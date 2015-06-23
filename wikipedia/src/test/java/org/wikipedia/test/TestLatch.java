package org.wikipedia.test;

import java.util.concurrent.CountDownLatch;

public class TestLatch {
    private final CountDownLatch latch;

    public TestLatch() {
        this(1);
    }

    public TestLatch(int count) {
        latch = new CountDownLatch(count);
    }

    public long getCount() {
        return latch.getCount();
    }

    public void countDown() {
        latch.countDown();
    }

    public void await() throws InterruptedException {
        if (!latch.await(TestConstants.TIMEOUT_DURATION, TestConstants.TIMEOUT_UNIT)) {
            throw new InterruptedException("Timeout elapsed.");
        }
    }
}
