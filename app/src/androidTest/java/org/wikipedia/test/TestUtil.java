package org.wikipedia.test;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;

public final class TestUtil {
    public static void runOnMainSync(@NonNull Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }

    private TestUtil() { }
}
