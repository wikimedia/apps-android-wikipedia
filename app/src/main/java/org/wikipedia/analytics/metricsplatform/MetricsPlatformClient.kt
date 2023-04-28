package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.ClientMetadata
import org.wikimedia.metrics_platform.MetricsClient

open class MetricsPlatformClient {
    private val clientMetadata: ClientMetadata = AndroidClientMetadata()
    open val client: MetricsClient = MetricsClient.builder(clientMetadata).build()
}