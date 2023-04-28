package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient

class MetricsPlatformClientPage(pageClientMetadata: AndroidPageClientMetadata) : MetricsPlatformClient() {
    override val client: MetricsClient = MetricsClient.builder(pageClientMetadata).build()
}