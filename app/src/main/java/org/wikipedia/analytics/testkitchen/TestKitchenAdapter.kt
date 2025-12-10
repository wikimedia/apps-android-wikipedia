package org.wikipedia.analytics.testkitchen

import android.os.Build
import org.wikimedia.testkitchen.EventSenderDefault
import org.wikimedia.testkitchen.LogAdapterImpl
import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.context.AgentData
import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.MediawikiData
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil

object TestKitchenAdapter {
    val agentData = AgentData(
        BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE,
        WikipediaApp.instance.appInstallID,
        WikipediaApp.instance.currentTheme.toString(),
        WikipediaApp.instance.versionCode,
        "WikipediaApp/" + BuildConfig.VERSION_NAME,
        "android",
        "app",
        Build.BRAND + " " + Build.MODEL,
        WikipediaApp.instance.languageState.systemLanguageCode,
        if (ReleaseUtil.isProdRelease) "prod" else "dev"
    )

    val mediawikiData = MediawikiData(
        WikipediaApp.instance.wikiSite.dbName(),
    )

    val domain = WikipediaApp.instance.wikiSite.authority()

    private val clientData = ClientData(
        agentData,
        null,
        mediawikiData,
        null,
        domain
    )

    val logger = LogAdapterImpl()

    val client = TestKitchenClient(
        clientData,
        EventSenderDefault(JsonUtil.json, OkHttpConnectionFactory.client, logger),
        null,
        queueCapacity = Prefs.analyticsQueueSize,
        logger = logger
    )
}
