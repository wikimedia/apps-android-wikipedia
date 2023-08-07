package org.wikipedia.analytics.metricsplatform

import org.wikipedia.util.ActiveTimer

open class TimedMetricsEvent : MetricsEvent() {
    val timer = ActiveTimer()
}
