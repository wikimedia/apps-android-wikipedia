package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikipedia.WikipediaApp

object MetricsPlatform {
    private val agentData: AgentData = AgentData(
        WikipediaApp.instance.appInstallID,
        "mobile app",
        "android"
    )

    private val clientData: ClientData = ClientData(
        agentData,
        null,
        null,
        WikipediaApp.instance.wikiSite.authority()
    )

    val client: MetricsClient = MetricsClient.builder(clientData).build()
}
