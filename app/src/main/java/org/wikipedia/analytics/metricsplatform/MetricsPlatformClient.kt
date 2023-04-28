package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.ClientMetadata
import org.wikimedia.metrics_platform.MetricsClient

object MetricsPlatformClient {
    private val clientMetadata: ClientMetadata = AndroidClientMetadata()
    val client: MetricsClient = MetricsClient.builder(clientMetadata).build()
}
