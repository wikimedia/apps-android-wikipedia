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
                /* eventName = */ EVENT_NAME_BASE + eventName,
                /* clientData = */ getClientData(pageData),
                /* interactionData = */ interactionData)
        }
    }

    protected fun getClientData(pageData: PageData?): ClientData {
        return ClientData(
            /* agentData = */ MetricsPlatform.agentData,
            /* pageData = */ pageData,
            /* mediawikiData = */ MetricsPlatform.mediawikiData,
            /* performerData = */ getPerformerData(),
            /* domain = */ MetricsPlatform.domain
        )
    }

    protected fun getPageData(fragment: PageFragment?): PageData? {
        val pageProperties = fragment?.page?.pageProperties ?: return null
        return PageData(
            /* id = */ pageProperties.pageId,
            /* title = */ fragment.model.title?.prefixedText.orEmpty(),
            /* namespaceId = */ pageProperties.namespace.code(),
            /* namespaceName = */ Namespace.of(pageProperties.namespace.code()).toString(),
            /* revisionId = */ pageProperties.revisionId,
            /* wikidataItemQid = */ pageProperties.wikiBaseItem.orEmpty(),
            /* contentLanguage = */ fragment.model.title?.wikiSite?.languageCode.orEmpty()
        )
    }

    protected fun getPageData(pageTitle: PageTitle?, pageId: Int = 0, revisionId: Long = 0): PageData? {
        if (pageTitle == null) return null
        return PageData(
            /* id = */ pageId,
            /* title = */ pageTitle.prefixedText,
            /* namespaceId = */ pageTitle.namespace().code(),
            /* namespaceName = */ Namespace.of(pageTitle.namespace().code()).toString(),
            /* revisionId = */ revisionId,
            /* wikidataItemQid = */ null,
            /* contentLanguage = */ pageTitle.wikiSite.languageCode
        )
    }

    private fun getPerformerData(): PerformerData {
        return PerformerData(
            /* id = */ AccountUtil.hashCode(),
            /* name = */ AccountUtil.userName,
            /* isLoggedIn = */ AccountUtil.isLoggedIn,
            /* isTemp = */ null,
            /* sessionId = */ EventPlatformClient.AssociationController.sessionId,
            /* pageviewId = */ EventPlatformClient.AssociationController.pageViewId,
            /* groups = */ AccountUtil.groups,
            /* languageGroups = */ WikipediaApp.instance.languageState.appLanguageCodes.toString(),
            /* languagePrimary = */ WikipediaApp.instance.languageState.appLanguageCode,
            /* registrationDt = */ null
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
