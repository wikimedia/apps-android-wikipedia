package org.wikimedia.metricsplatform

import org.wikimedia.metricsplatform.config.StreamConfig
import org.wikimedia.metricsplatform.context.AgentData
import org.wikimedia.metricsplatform.context.ClientData
import org.wikimedia.metricsplatform.context.ContextValue
import org.wikimedia.metricsplatform.context.MediawikiData
import org.wikimedia.metricsplatform.context.PageData
import org.wikimedia.metricsplatform.context.PerformerData
import org.wikimedia.metricsplatform.event.EventProcessed

class ContextController {
    fun enrichEvent(event: EventProcessed, streamConfig: StreamConfig) {
        if (!streamConfig.hasRequestedContextValuesConfig()) {
            return
        }
        // Check stream config for which contextual values should be added to the event.
        val requestedValuesFromConfig = streamConfig.producerConfig?.metricsPlatformClientConfig?.requestedValues.orEmpty()
        // Add required properties.
        val requestedValues= mutableSetOf<String>()
        requestedValues.addAll(requestedValuesFromConfig)
        requestedValues.addAll(REQUIRED_PROPERTIES)
        val filteredData = filterClientData(event.clientData, requestedValues)
        event.applyClientData(filteredData)
    }

    private fun filterClientData(clientData: ClientData, requestedValues: Collection<String>): ClientData {
        val newAgentData = AgentData()
        val newPageData = PageData()
        val newMediawikiData = MediawikiData()
        val newPerformerData = PerformerData()

        for (requestedValue in requestedValues) {
            when (requestedValue) {
                ContextValue.AGENT_APP_INSTALL_ID -> newAgentData.appInstallId = clientData.agentData?.appInstallId
                ContextValue.AGENT_CLIENT_PLATFORM -> newAgentData.clientPlatform = clientData.agentData?.clientPlatform
                ContextValue.AGENT_CLIENT_PLATFORM_FAMILY -> newAgentData.clientPlatformFamily = clientData.agentData?.clientPlatformFamily
                ContextValue.AGENT_APP_FLAVOR -> newAgentData.appFlavor = clientData.agentData?.appFlavor
                ContextValue.AGENT_APP_THEME -> newAgentData.appTheme = clientData.agentData?.appTheme
                ContextValue.AGENT_APP_VERSION -> newAgentData.appVersion = clientData.agentData?.appVersion
                ContextValue.AGENT_APP_VERSION_NAME -> newAgentData.appVersionName = clientData.agentData?.appVersionName
                ContextValue.AGENT_DEVICE_FAMILY -> newAgentData.deviceFamily = clientData.agentData?.deviceFamily
                ContextValue.AGENT_DEVICE_LANGUAGE -> newAgentData.deviceLanguage = clientData.agentData?.deviceLanguage
                ContextValue.AGENT_RELEASE_STATUS -> newAgentData.releaseStatus = clientData.agentData?.releaseStatus
                ContextValue.PAGE_ID -> newPageData.id = clientData.pageData?.id
                ContextValue.PAGE_TITLE -> newPageData.title = clientData.pageData?.title
                ContextValue.PAGE_NAMESPACE_ID -> newPageData.namespaceId = clientData.pageData?.namespaceId
                ContextValue.PAGE_NAMESPACE_NAME -> newPageData.namespaceName = clientData.pageData?.namespaceName
                ContextValue.PAGE_REVISION_ID -> newPageData.revisionId = clientData.pageData?.revisionId
                ContextValue.PAGE_WIKIDATA_QID -> newPageData.wikidataItemQid = clientData.pageData?.wikidataItemQid
                ContextValue.PAGE_CONTENT_LANGUAGE -> newPageData.contentLanguage = clientData.pageData?.contentLanguage
                ContextValue.MEDIAWIKI_DATABASE -> newMediawikiData.database = clientData.mediawikiData?.database
                ContextValue.PERFORMER_ID -> newPerformerData.id = clientData.performerData?.id
                ContextValue.PERFORMER_NAME -> newPerformerData.name = clientData.performerData?.name
                ContextValue.PERFORMER_IS_LOGGED_IN -> newPerformerData.isLoggedIn = clientData.performerData?.isLoggedIn
                ContextValue.PERFORMER_IS_TEMP -> newPerformerData.isTemp = clientData.performerData?.isTemp
                ContextValue.PERFORMER_SESSION_ID -> newPerformerData.sessionId = clientData.performerData?.sessionId
                ContextValue.PERFORMER_PAGEVIEW_ID -> newPerformerData.pageviewId = clientData.performerData?.pageviewId
                ContextValue.PERFORMER_GROUPS -> newPerformerData.groups = clientData.performerData?.groups
                ContextValue.PERFORMER_LANGUAGE_GROUPS -> {
                    var languageGroups = clientData.performerData?.languageGroups
                    if (languageGroups != null && languageGroups.length > 255) {
                        languageGroups = languageGroups.substring(0, 255)
                    }
                    newPerformerData.languageGroups = languageGroups
                }

                ContextValue.PERFORMER_LANGUAGE_PRIMARY -> newPerformerData.languagePrimary = clientData.performerData?.languagePrimary
                ContextValue.PERFORMER_REGISTRATION_DT -> newPerformerData.registrationDt = clientData.performerData?.registrationDt
                else -> throw IllegalArgumentException("Unknown property: $requestedValue")
            }
        }

        return ClientData(newAgentData, newPageData, newMediawikiData, newPerformerData)
    }

    companion object {
        /**
         * @see [Metrics Platform/Contextual attributes](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Contextual_attributes)
         */
        private val REQUIRED_PROPERTIES = listOf(
            "agent_app_flavor",
            "agent_app_install_id",
            "agent_app_theme",
            "agent_app_version",
            "agent_app_version_name",
            "agent_client_platform",
            "agent_client_platform_family",
            "agent_device_family",
            "agent_device_language",
            "agent_release_status"
        )
    }
}
