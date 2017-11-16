package org.wikipedia.util;

import android.support.annotation.NonNull;

public final class MathUtil {

    private static final int PERCENTAGE_BASE = 100;

    public static float constrain(float f, float min, float max) {
        return Math.min(Math.max(min, f), max);
    }

    public static class Averaged<T extends Number> {
        private double sampleSum;
        private int sampleSize;

        public void addSample(T sample) {
            sampleSum += sample.doubleValue();
            ++sampleSize;
        }

        public double getAverage() {
            return sampleSize == 0 ? 0 : sampleSum / sampleSize;
        }

        public void reset() {
            sampleSum = 0;
            sampleSize = 0;
        }
    }

    public static int percentage(@NonNull float numerator, @NonNull float denominator) {
        return (int) (numerator / denominator * PERCENTAGE_BASE);
    }

    private MathUtil() {
    }
}
