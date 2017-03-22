package org.wikipedia.testlib;

import org.wikipedia.util.ReleaseUtil;

import java.util.concurrent.TimeUnit;

public final class TestConstants {
    public static final int TIMEOUT_DURATION = 5;
    public static final TimeUnit TIMEOUT_UNIT = ReleaseUtil.isDevRelease() ? TimeUnit.SECONDS : TimeUnit.MINUTES;

    private TestConstants() { }
}
