package org.wikipedia.analytics.testkitchen

import android.os.Build
import org.wikimedia.testkitchen.EventSenderDefault
import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.context.AgentData
import org.wikimedia.testkitchen.context.ClientDataCallback
import org.wikimedia.testkitchen.context.MediawikiData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.context.PerformerData
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil

object TestKitchenAdapter : ClientDataCallback {
    val logger = LogAdapterImpl()

    val client = TestKitchenClient(
        eventSender = EventSenderDefault(JsonUtil.json, OkHttpConnectionFactory.client, logger),
        sourceConfigInit = null,
        clientDataCallback = this,
        queueCapacity = Prefs.analyticsQueueSize,
        logger = logger,
        isDebug = ReleaseUtil.isDevRelease
    )

    override fun getAgentData(): AgentData {
        return AgentData(
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
    }

    override fun getPageData(): PageData? {
        // TODO
        return null
    }

    override fun getMediawikiData(): MediawikiData {
        return MediawikiData(
            WikipediaApp.instance.wikiSite.dbName(),
        )
    }

    override fun getPerformerData(): PerformerData {
        return PerformerData(
            isLoggedIn = AccountUtil.isLoggedIn,
            isTemp = AccountUtil.isTemporaryAccount,
            sessionId = client.sessionController.sessionId,
            languagePrimary = WikipediaApp.instance.appOrSystemLanguageCode
        )
    }

    override fun getDomain(): String {
        return WikipediaApp.instance.wikiSite.authority()
    }

    fun getPageData(fragment: PageFragment?): PageData? {
        val pageProperties = fragment?.page?.pageProperties ?: return null
        return PageData(
            pageProperties.pageId,
            fragment.model.title?.prefixedText.orEmpty(),
            pageProperties.namespace.code(),
            Namespace.of(pageProperties.namespace.code()).toString(),
            pageProperties.revisionId,
            pageProperties.wikiBaseItem.orEmpty(),
            fragment.model.title?.wikiSite?.languageCode.orEmpty()
        )
    }

    fun getPageData(pageTitle: PageTitle?, pageId: Int = 0, revisionId: Long = 0): PageData? {
        if (pageTitle == null) return null
        return PageData(
            pageId,
            pageTitle.prefixedText,
            pageTitle.namespace().code(),
            Namespace.of(pageTitle.namespace().code()).toString(),
            revisionId,
            null,
            pageTitle.wikiSite.languageCode
        )
    }

    fun getPageData(pageTitle: PageTitle?, pageSummary: PageSummary?): PageData? {
        if (pageTitle == null) return null
        return PageData(
            pageSummary?.pageId,
            pageTitle.prefixedText,
            pageTitle.namespace().code(),
            Namespace.of(pageTitle.namespace().code()).toString(),
            pageSummary?.revision,
            pageSummary?.wikiBaseItem,
            pageTitle.wikiSite.languageCode
        )
    }
}
