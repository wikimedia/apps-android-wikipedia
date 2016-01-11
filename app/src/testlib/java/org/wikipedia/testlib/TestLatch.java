package org.wikipedia.testlib;

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

    public void await() {
        boolean done = false;

        try {
            done = latch.await(TestConstants.TIMEOUT_DURATION, TestConstants.TIMEOUT_UNIT);
        } catch (InterruptedException ignore) { }

        if (!done) {
            throw new RuntimeException("Timeout elapsed.");
        }
    }
}
