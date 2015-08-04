package org.wikipedia.util;

public final class MathUtil {

    public static float constrain(float f, float min, float max) {
        return Math.min(Math.max(min, f), max);
    }

    private MathUtil() {
    }
}