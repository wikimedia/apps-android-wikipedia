package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.MediawikiData
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import java.time.Duration

object MetricsPlatform {
    val agentData = AgentData(
        WikipediaApp.instance.appInstallID,
        "android",
        "app"
    )

    val mediawikiData = MediawikiData(
        WikipediaApp.instance.currentTheme.toString(),
        WikipediaApp.instance.versionCode.toString(),
        ReleaseUtil.isProdRelease,
        ReleaseUtil.isDevRelease,
        WikipediaApp.instance.wikiSite.dbName(),
        WikipediaApp.instance.languageState.systemLanguageCode,
        null
    )

    val domain = WikipediaApp.instance.wikiSite.authority()

    private val clientData = ClientData(
        agentData,
        null,
        mediawikiData,
        null,
        domain
    )

    val client: MetricsClient = MetricsClient.builder(clientData)
        .eventQueueCapacity(Prefs.analyticsQueueSize)
        .streamConfigFetchInterval(Duration.ofHours(12))
        .sendEventsInterval(Duration.ofSeconds(30))
        .isDebug(ReleaseUtil.isDevRelease)
        .build()
}
