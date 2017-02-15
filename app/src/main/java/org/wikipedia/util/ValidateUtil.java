package org.wikipedia.util;

import org.apache.commons.lang3.Validate;

public final class ValidateUtil {
    public static void noNullElements(Object... objs) {
        Validate.noNullElements(objs);
    }

    private ValidateUtil() { }
}
