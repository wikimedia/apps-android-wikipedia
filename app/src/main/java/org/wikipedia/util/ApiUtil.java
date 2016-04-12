package org.wikipedia.util;

import android.os.Build;

// TODO: replace usage of this class with inline checks. The IDE omits warnings when these methods
//       are inlined.
@Deprecated
public final class ApiUtil {
    /** @return True if SDK API level is greater than or equal to 23. */
    public static boolean hasMarshmallow() {
        return has(Build.VERSION_CODES.M);
    }

    /** @return True if SDK API level is greater than or equal to 22. */
    public static boolean hasLollipopMr1() {
        return has(Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    /** @return True if SDK API level is greater than or equal to 21. */
    public static boolean hasLollipop() {
        return has(Build.VERSION_CODES.LOLLIPOP);
    }

    /** @return True if SDK API level is greater than or equal to 19. */
    public static boolean hasKitKat() {
        return has(Build.VERSION_CODES.KITKAT);
    }

    /** @return True if SDK API level is greater than or equal to 18. */
    public static boolean hasJellyBeanMr2() {
        return has(Build.VERSION_CODES.JELLY_BEAN_MR2);
    }

    /** @return True if SDK API level is greater than or equal to 17. */
    public static boolean hasJellyBeanMr1() {
        return has(Build.VERSION_CODES.JELLY_BEAN_MR1);
    }

    /** @return True if SDK API level is greater than or equal to 16. */
    public static boolean hasJellyBean() {
        return has(Build.VERSION_CODES.JELLY_BEAN);
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

    private ApiUtil() { }
}
