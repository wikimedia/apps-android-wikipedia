package org.wikipedia.test.perf;

import org.wikipedia.util.log.L;

import android.os.SystemClock;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple performance test measurement collection mechanism for Android.
 */
public class MeasurementController {
    private final Map<String, MeasurementSeries> seriesMap = new ArrayMap<>();

    public void start(String key) {
        MeasurementSeries measurementSeries = seriesMap.get(key);
        if (measurementSeries == null) {
            measurementSeries = new MeasurementSeries();
            seriesMap.put(key, measurementSeries);
        }
        if (measurementSeries.currentStart > 0) {
            L.w("Overwriting currentStart of " + measurementSeries.currentStart);
        }
        L.v("Start(" + key + ")");
        measurementSeries.currentStart = SystemClock.elapsedRealtime();
    }

    public void stop(String key) {
        MeasurementSeries measurementSeries = seriesMap.get(key);
        if (measurementSeries == null) {
            throw new IllegalStateException("Stop called without start");
        }

        long duration = SystemClock.elapsedRealtime() - measurementSeries.currentStart;
        measurementSeries.currentStart = 0L;
        measurementSeries.measurements.add(duration);

        L.v("Duration(" + key + ") = " + MeasurementSeries.toMillisecondString(duration));
    }

    public void analyzeAll() {
        L.i("---");
        for (String key : seriesMap.keySet()) {
            analyze(key);
        }
        L.i("---");
    }

    private void analyze(String key) {
        L.i(key + ": " + seriesMap.get(key).analyze());
    }

    /**
     * One series of similar tests which can be aggregated
     */
    public static class MeasurementSeries {
        private List<Long> measurements = new ArrayList<>();
        private long currentStart = 0L;

        public String analyze() {
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            long sum = 0L;

            if (measurements.isEmpty()) {
                return "No measurements to analyze";
            }

            for (long current : measurements) {
                if (current < min) {
                    min = current;
                }
                if (current > max) {
                    max = current;
                }
                sum += current;
            }

            String res = toMillisecondString(sum / measurements.size());
            res += "(n: " + measurements.size()
                    + "; min: " + toMillisecondString(min)
                    + "; max: " + toMillisecondString(max)
                    + ")";
            return res;
        }

        static String toMillisecondString(long value) {
            return value + "ms";
        }
    }
}
