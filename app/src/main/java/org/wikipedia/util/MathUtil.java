package org.wikipedia.util;

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

    public static float percentage(float numerator, float denominator) {
        return numerator / denominator * PERCENTAGE_BASE;
    }

    private MathUtil() {
    }
}
