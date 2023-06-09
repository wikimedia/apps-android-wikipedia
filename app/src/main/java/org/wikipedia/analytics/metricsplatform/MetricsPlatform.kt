package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import java.time.Duration

object MetricsPlatform {
    private val agentData = AgentData(
        WikipediaApp.instance.appInstallID,
        "android",
        "app"
    )

    private val clientData = ClientData(
        agentData,
        null,
        null,
        WikipediaApp.instance.wikiSite.authority()
    )

    val client: MetricsClient = MetricsClient.builder(clientData)
        .eventQueueCapacity(Prefs.analyticsQueueSize)
        .streamConfigFetchInterval(Duration.ofHours(12))
        .sendEventsInterval(Duration.ofSeconds(30))
        .build()
}
