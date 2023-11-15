package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.MetricsClient
import org.wikimedia.metrics_platform.context.AgentData
import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.MediawikiData
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil
import java.time.Duration

object MetricsPlatform {
    val agentData = AgentData(
        /* appFlavor = */ BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE,
        /* appInstallId = */ WikipediaApp.instance.appInstallID,
        /* appTheme = */ WikipediaApp.instance.currentTheme.toString(),
        /* appVersion = */ WikipediaApp.instance.versionCode.toString(),
        /* clientPlatform = */ "android",
        /* clientPlatformFamily = */ "app",
        /* deviceLanguage = */ WikipediaApp.instance.languageState.systemLanguageCode,
        /* releaseStatus = */ if (ReleaseUtil.isProdRelease) "prod" else "dev"
    )

    val mediawikiData = MediawikiData(
        /* database = */ WikipediaApp.instance.wikiSite.dbName(),
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
