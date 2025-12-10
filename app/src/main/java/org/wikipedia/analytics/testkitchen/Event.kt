package org.wikipedia.analytics.testkitchen

import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.context.PerformerData
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle

open class Event {

    /**
     * Submit an event to the Metrics Platform using the base interaction schema
     *
     * @param streamName the name of the stream
     * @param eventName the name of the event
     * @param interactionData a data object that conforms to core interactions
     * @param pageData dynamic page data that should be added to the ClientData object
     */
    protected fun submitEvent(
        streamName: String,
        eventName: String,
        interactionData: InteractionData? = null,
        pageData: PageData? = null
    ) {
        TestKitchenAdapter.client.submitInteraction(streamName,
            EVENT_NAME_BASE + eventName,
            clientData = getClientData(pageData),
            interactionData = interactionData)
    }

    private fun getClientData(pageData: PageData?): ClientData {
        return ClientData(
            TestKitchenAdapter.agentData,
            pageData,
            TestKitchenAdapter.mediawikiData,
            getPerformerData(),
            TestKitchenAdapter.domain
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

    protected fun getPageData(pageTitle: PageTitle?, pageSummary: PageSummary?): PageData? {
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

    private fun getPerformerData(): PerformerData {
        return PerformerData(
            AccountUtil.hashCode(),
            AccountUtil.userName,
            AccountUtil.isLoggedIn,
            AccountUtil.isTemporaryAccount,
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
        actionSubtype: String? = null,
        actionSource: String? = null,
        actionContext: String? = null,
        elementId: String? = null,
        elementFriendlyName: String? = null,
        funnelEntryToken: String? = null,
        funnelEventSequencePosition: Int? = null
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
