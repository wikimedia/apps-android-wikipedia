package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikipedia.WikipediaApp
import java.time.Duration

object MetricsPlatform {
    private val agentData = AgentData(
        WikipediaApp.instance.appInstallID,
        "mobile app",
        "android"
    )

    private val clientData = ClientData(
        agentData,
        null,
        null,
        WikipediaApp.instance.wikiSite.authority()
    )

    val client: MetricsClient = MetricsClient.builder(clientData)
        .eventQueueCapacity(256)
        .streamConfigFetchInterval(Duration.ofHours(12))
        .sendEventsInterval(Duration.ofSeconds(30))
        .build()
}
