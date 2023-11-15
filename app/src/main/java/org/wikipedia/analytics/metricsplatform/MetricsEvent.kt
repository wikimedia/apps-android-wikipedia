package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.InteractionData
import org.wikimedia.metrics_platform.context.PageData
import org.wikimedia.metrics_platform.context.PerformerData
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil

open class MetricsEvent {

    protected fun submitEvent(eventName: String, interactionData: InteractionData?, pageData: PageData? = null) {
        if (ReleaseUtil.isPreProdRelease && Prefs.isEventLoggingEnabled) {
            MetricsPlatform.client.submitInteraction(
                EVENT_NAME_BASE + eventName,
                getClientData(pageData),
                interactionData)
        }
    }

    protected fun getClientData(pageData: PageData?): ClientData {
        return ClientData(
            MetricsPlatform.agentData,
            pageData,
            MetricsPlatform.mediawikiData,
            getPerformerData(),
            MetricsPlatform.domain
        )
    }

    protected fun getPageData(fragment: PageFragment?): PageData? {
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

    protected fun getPageData(pageTitle: PageTitle?, pageId: Int = 0, revisionId: Long = 0): PageData? {
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

    private fun getPerformerData(): PerformerData {
        return PerformerData(
            AccountUtil.hashCode(),
            AccountUtil.userName,
            AccountUtil.isLoggedIn,
            null,
            EventPlatformClient.AssociationController.sessionId,
            EventPlatformClient.AssociationController.pageViewId,
            AccountUtil.groups,
            WikipediaApp.instance.languageState.appLanguageCodes.toString(),
            WikipediaApp.instance.languageState.appLanguageCode,
            null
        )
    }

    protected fun getInteractionData(
        action: String,
        actionSubtype: String?,
        actionSource: String?,
        actionContext: String?,
        elementId: String?,
        elementFriendlyName: String?,
        funnelEntryToken: String?,
        funnelEventSequencePosition: Int?
    ): InteractionData {
        return InteractionData(
            action,
            actionSubtype,
            actionSource,
            actionContext,
            elementId,
            elementFriendlyName,
            funnelEntryToken,
            funnelEventSequencePosition
        )
    }

    companion object {
        private const val EVENT_NAME_BASE = "android.metrics_platform."
    }
}
