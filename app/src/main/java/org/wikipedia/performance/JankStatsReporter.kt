package org.wikipedia.performance

import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Window
import androidx.annotation.UiThread
import androidx.metrics.performance.FrameData
import androidx.metrics.performance.FrameDataApi24
import androidx.metrics.performance.FrameDataApi31
import androidx.metrics.performance.JankStats
import androidx.metrics.performance.StateInfo
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/**
 * Collects JankStats frame data for a window, grouped by each PerformanceMetricsState key/value
 * pair that was active during the frame. Every janky frame is logged as it happens, and a summary
 * table is logged (and the collected data reset) when tracking stops.
 *
 * Logs go directly to logcat with a fixed tag, so that output can be filtered the same way in
 * every build flavor: `adb logcat -s WikiJankStats`
 */
class JankStatsReporter @UiThread constructor(window: Window, private val name: String) {
    // Frame data arrives on a background FrameMetrics thread on API 24+, so all access to the
    // collected stats is synchronized on this lock.
    private val lock = Any()
    private var allFrames = FrameStats()
    private val framesByState = mutableMapOf<StateInfo, FrameStats>()
    private var trackingStartMillis = 0L

    private val jankStats = JankStats.createAndTrack(window, ::onFrame).also {
        it.isTrackingEnabled = false
    }

    @UiThread
    fun startTracking() {
        synchronized(lock) {
            trackingStartMillis = SystemClock.elapsedRealtime()
        }
        jankStats.isTrackingEnabled = true
    }

    @UiThread
    fun stopTrackingAndLogSummary() {
        jankStats.isTrackingEnabled = false
        val summary = synchronized(lock) {
            buildSummary(SystemClock.elapsedRealtime() - trackingStartMillis).also {
                allFrames = FrameStats()
                framesByState.clear()
            }
        }
        summary.forEach { Log.i(TAG, it) }
    }

    // The FrameData object is reused by JankStats, so it must not be retained past this call.
    private fun onFrame(frameData: FrameData) {
        synchronized(lock) {
            allFrames.record(frameData)
            frameData.states.forEach { framesByState.getOrPut(it) { FrameStats() }.record(frameData) }
        }
        if (frameData.isJank) {
            Log.d(TAG, describeJankyFrame(frameData))
        }
    }

    private fun describeJankyFrame(frameData: FrameData): String {
        val durations = buildList {
            add("ui=${formatMillis(frameData.frameDurationUiNanos)}")
            if (frameData is FrameDataApi24) {
                add("cpu=${formatMillis(frameData.frameDurationCpuNanos)}")
            }
            if (frameData is FrameDataApi31) {
                add("total=${formatMillis(frameData.frameDurationTotalNanos)}")
                add("overrun=${formatMillis(frameData.frameOverrunNanos)}")
            }
        }
        val states = frameData.states.joinToString { "${it.key}=${it.value}" }
        return "[$name] Janky frame: ${durations.joinToString(" ")} states=[$states]"
    }

    private fun buildSummary(trackedMillis: Long): List<String> {
        val rows = listOf(ALL_FRAMES_LABEL to allFrames) +
                framesByState.entries
                    .sortedWith(compareByDescending<Map.Entry<StateInfo, FrameStats>> { it.value.jankCount }
                        .thenByDescending { it.value.frameCount })
                    .map { "${it.key.key}=${it.key.value}" to it.value }
        return listOf(
            "[$name] JankStats summary: ${TimeUnit.MILLISECONDS.toSeconds(trackedMillis)}s tracked. " +
                    "Durations are UI-thread time in ms. 'missed' is frames past their deadline (API 31+).",
            String.format(Locale.ROOT, ROW_FORMAT, "state", "frames", "janky", "jank%", "missed", "p50", "p90", "p99", "max")
        ) + rows.map { (label, stats) -> stats.toSummaryRow(label) }
    }

    private class FrameStats {
        var frameCount = 0
            private set
        var jankCount = 0
            private set
        private var missedDeadlineCount = 0
        private var maxUiDurationNanos = 0L
        private val uiDurationMillisHistogram = IntArray(HISTOGRAM_MAX_MILLIS + 1)

        fun record(frameData: FrameData) {
            frameCount++
            if (frameData.isJank) {
                jankCount++
            }
            if (frameData is FrameDataApi31 && frameData.frameOverrunNanos > 0) {
                missedDeadlineCount++
            }
            maxUiDurationNanos = maxOf(maxUiDurationNanos, frameData.frameDurationUiNanos)
            val bucket = TimeUnit.NANOSECONDS.toMillis(frameData.frameDurationUiNanos).coerceIn(0, HISTOGRAM_MAX_MILLIS.toLong()).toInt()
            uiDurationMillisHistogram[bucket]++
        }

        fun toSummaryRow(label: String): String {
            val jankPercent = if (frameCount > 0) String.format(Locale.ROOT, "%.1f", jankCount * 100.0 / frameCount) else "-"
            val missed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) missedDeadlineCount.toString() else "-"
            return String.format(Locale.ROOT, ROW_FORMAT, label.take(LABEL_MAX_LENGTH), frameCount, jankCount, jankPercent, missed,
                percentileMillis(50), percentileMillis(90), percentileMillis(99), formatMillis(maxUiDurationNanos))
        }

        private fun percentileMillis(percentile: Int): String {
            if (frameCount == 0) {
                return "-"
            }
            val threshold = ceil(frameCount * percentile / 100.0).toInt()
            var cumulativeCount = 0
            uiDurationMillisHistogram.forEachIndexed { millis, count ->
                cumulativeCount += count
                if (cumulativeCount >= threshold) {
                    return if (millis == HISTOGRAM_MAX_MILLIS) "$millis+" else millis.toString()
                }
            }
            return "$HISTOGRAM_MAX_MILLIS+"
        }
    }

    companion object {
        private const val TAG = "WikiJankStats"
        private const val ALL_FRAMES_LABEL = "(all frames)"
        private const val HISTOGRAM_MAX_MILLIS = 250
        private const val LABEL_MAX_LENGTH = 48
        private const val ROW_FORMAT = "%-48s %7s %6s %6s %6s %5s %5s %5s %7s"

        private fun formatMillis(nanos: Long): String {
            return String.format(Locale.ROOT, "%.1f", nanos / 1_000_000.0)
        }
    }
}
