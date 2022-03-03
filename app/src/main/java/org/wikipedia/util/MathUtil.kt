package org.wikipedia.util

object MathUtil {
    private const val PERCENTAGE_BASE = 100

    fun percentage(numerator: Float, denominator: Float): Float {
        return numerator / denominator * PERCENTAGE_BASE
    }

    class Averaged<T : Number?> {
        private var sampleSum = 0.0
        private var sampleSize = 0
        fun addSample(sample: T) {
            sampleSum += sample!!.toDouble()
            ++sampleSize
        }

        val average: Double
            get() = if (sampleSize == 0) 0.0 else sampleSum / sampleSize

        fun reset() {
            sampleSum = 0.0
            sampleSize = 0
        }
    }
}
