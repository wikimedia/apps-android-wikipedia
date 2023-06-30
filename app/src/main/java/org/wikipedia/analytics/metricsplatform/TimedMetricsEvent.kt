package org.wikipedia.analytics.metricsplatform

import org.wikipedia.util.ActiveTimer

open class TimedMetricsEvent : MetricsEvent() {
    protected val timer = ActiveTimer()
}
