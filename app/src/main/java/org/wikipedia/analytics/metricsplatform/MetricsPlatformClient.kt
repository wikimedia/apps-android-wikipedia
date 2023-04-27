package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
object MetricsPlatformClient {
    val client: MetricsClient = MetricsClient.builder(AndroidClientMetadata).build()
}