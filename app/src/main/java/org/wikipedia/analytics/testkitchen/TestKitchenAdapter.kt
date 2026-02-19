package org.wikipedia.analytics.testkitchen

import android.os.Build
import org.wikimedia.testkitchen.EventSender
import org.wikimedia.testkitchen.TestKitchenClient
import org.wikimedia.testkitchen.config.DestinationEventService
import org.wikimedia.testkitchen.context.AgentData
import org.wikimedia.testkitchen.context.ClientDataCallback
import org.wikimedia.testkitchen.context.MediawikiData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.context.PerformerData
import org.wikimedia.testkitchen.event.Event
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L

object TestKitchenAdapter : ClientDataCallback, EventSender {
    val logger = LogAdapterImpl()

    val client = TestKitchenClient(
        eventSender = this,
        sourceConfigInit = null,
        clientDataCallback = this,
        logger = logger
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

    override suspend fun sendEvents(destinationEventService: DestinationEventService, events: List<Event>) {
        val response = if (ReleaseUtil.isDevRelease) ServiceFactory.getAnalyticsRest(destinationEventService).postEventsTk(events) else
            ServiceFactory.getAnalyticsRest(destinationEventService).postEventsHastyTk(events)
        L.d("${events.size} events sent successfully (${response.code()})")
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

    fun getPageData(pageTitle: PageTitle?, pageId: Int? = null, revisionId: Long? = null): PageData? {
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
