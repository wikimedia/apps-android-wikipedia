package org.wikipedia.util;

import android.os.Build;

public final class ApiUtil {
    /** @return True if SDK API level is greater than or equal to 11. */
    public static boolean hasHoneyComb() {
        return has(Build.VERSION_CODES.HONEYCOMB);
    }

    /** @return SDK level. */
    private static int getLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }

    /** @return True if SDK API is less than level. */
    private static boolean isBefore(int level) {
        return getLevel() < level;
    }

    /** @return True if SDK API  to level. */
    private static boolean has(int level) {
        return !isBefore(level);
    }

    private ApiUtil() {
    }
}