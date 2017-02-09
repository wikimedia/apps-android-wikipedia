package org.wikipedia.util;

public final class MathUtil {

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

    private MathUtil() {
    }
}
