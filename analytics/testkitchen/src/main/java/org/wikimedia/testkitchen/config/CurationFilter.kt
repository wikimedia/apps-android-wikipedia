@file:UseSerializers(InstantSerializer::class)

package org.wikimedia.testkitchen.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.wikimedia.testkitchen.config.curation.CollectionCurationRules
import org.wikimedia.testkitchen.config.curation.ComparableCurationRules
import org.wikimedia.testkitchen.config.curation.CurationRules
import org.wikimedia.testkitchen.context.AgentData
import org.wikimedia.testkitchen.context.ContextValue
import org.wikimedia.testkitchen.context.InstantSerializer
import org.wikimedia.testkitchen.context.MediawikiData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.context.PerformerData
import org.wikimedia.testkitchen.event.Event
import java.time.Instant
import java.util.function.Predicate

@Serializable
class CurationFilter : Predicate<Event> {
    @SerialName(ContextValue.AGENT_APP_INSTALL_ID) var agentAppInstallIdRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_CLIENT_PLATFORM) var agentClientPlatformRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_CLIENT_PLATFORM_FAMILY) var agentClientPlatformFamilyRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_APP_FLAVOR) var agentAppFlavorRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_APP_THEME) var agentAppThemeRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_APP_VERSION) var agentAppVersionRules: CurationRules<Int>? = null
    @SerialName(ContextValue.AGENT_APP_VERSION_NAME) var agentAppVersionNameRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_DEVICE_FAMILY) var agentDeviceFamilyRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_DEVICE_LANGUAGE) var agentDeviceLanguageRules: CurationRules<String>? = null
    @SerialName(ContextValue.AGENT_RELEASE_STATUS) var agentReleaseStatusRules: CurationRules<String>? = null
    @SerialName(ContextValue.MEDIAWIKI_DATABASE) var mediawikiDatabase: CurationRules<String>? = null
    @SerialName(ContextValue.PAGE_ID) var pageIdRules: ComparableCurationRules<Int>? = null
    @SerialName(ContextValue.PAGE_NAMESPACE_ID) var pageNamespaceIdRules: ComparableCurationRules<Int>? = null
    @SerialName(ContextValue.PAGE_NAMESPACE_NAME) var pageNamespaceNameRules: CurationRules<String>? = null
    @SerialName(ContextValue.PAGE_TITLE) var pageTitleRules: CurationRules<String>? = null
    @SerialName(ContextValue.PAGE_REVISION_ID) var pageRevisionIdRules: ComparableCurationRules<Long>? = null
    @SerialName(ContextValue.PAGE_WIKIDATA_QID) var pageWikidataQidRules: CurationRules<String>? = null
    @SerialName(ContextValue.PAGE_CONTENT_LANGUAGE) var pageContentLanguageRules: CurationRules<String>? = null
    @SerialName(ContextValue.PERFORMER_ID) var performerIdRules: ComparableCurationRules<Int>? = null
    @SerialName(ContextValue.PERFORMER_NAME) var performerNameRules: CurationRules<String>? = null
    @SerialName(ContextValue.PERFORMER_SESSION_ID) var performerSessionIdRules: CurationRules<String>? = null
    @SerialName(ContextValue.PERFORMER_PAGEVIEW_ID) var performerPageviewIdRules: CurationRules<String>? = null
    @SerialName(ContextValue.PERFORMER_GROUPS) var performerGroupsRules: CollectionCurationRules<String>? = null
    @SerialName(ContextValue.PERFORMER_IS_LOGGED_IN) var performerIsLoggedInRules: CurationRules<Boolean>? = null
    @SerialName(ContextValue.PERFORMER_IS_TEMP) var performerIsTempRules: CurationRules<Boolean>? = null
    @SerialName(ContextValue.PERFORMER_REGISTRATION_DT) var performerRegistrationDtRules: ComparableCurationRules<Instant>? = null
    @SerialName(ContextValue.PERFORMER_LANGUAGE_GROUPS) var performerLanguageGroupsRules: CurationRules<String>? = null
    @SerialName(ContextValue.PERFORMER_LANGUAGE_PRIMARY) var performerLanguagePrimaryRules: CurationRules<String>? = null

    override fun test(event: Event): Boolean {
        return applyAgentRules(event.agentData) &&
                applyMediaWikiRules(event.mediawikiData) &&
                applyPageRules(event.pageData) &&
                applyPerformerRules(event.performerData)
    }

    private fun applyAgentRules(data: AgentData?): Boolean {
        return applyPredicate(this.agentAppInstallIdRules, data?.appInstallId) &&
                applyPredicate(this.agentClientPlatformRules, data?.clientPlatform) &&
                applyPredicate(this.agentClientPlatformFamilyRules, data?.clientPlatformFamily) &&
                applyPredicate(this.agentAppFlavorRules, data?.appFlavor) &&
                applyPredicate(this.agentAppThemeRules, data?.appTheme) &&
                applyPredicate(this.agentAppVersionRules, data?.appVersion) &&
                applyPredicate(this.agentAppVersionNameRules, data?.appVersionName) &&
                applyPredicate(this.agentDeviceFamilyRules, data?.deviceFamily) &&
                applyPredicate(this.agentDeviceLanguageRules, data?.deviceLanguage) &&
                applyPredicate(this.agentReleaseStatusRules, data?.releaseStatus)
    }

    private fun applyMediaWikiRules(data: MediawikiData?): Boolean {
        return applyPredicate(this.mediawikiDatabase, data?.database)
    }

    private fun applyPageRules(data: PageData?): Boolean {
        return applyPredicate(this.pageIdRules, data?.id) &&
                applyPredicate(this.pageNamespaceIdRules, data?.namespaceId) &&
                applyPredicate(this.pageNamespaceNameRules, data?.namespaceName) &&
                applyPredicate(this.pageTitleRules, data?.title) &&
                applyPredicate(this.pageRevisionIdRules, data?.revisionId) &&
                applyPredicate(this.pageWikidataQidRules, data?.wikidataItemQid) &&
                applyPredicate(this.pageContentLanguageRules, data?.contentLanguage)
    }

    private fun applyPerformerRules(data: PerformerData?): Boolean {
        return applyPredicate(this.performerIdRules, data?.id) &&
                applyPredicate(this.performerNameRules, data?.name) &&
                applyPredicate(this.performerSessionIdRules, data?.sessionId) &&
                applyPredicate(this.performerPageviewIdRules, data?.pageviewId) &&
                applyPredicate(this.performerGroupsRules, data?.groups) &&
                applyPredicate(this.performerIsLoggedInRules, data?.isLoggedIn) &&
                applyPredicate(this.performerIsTempRules, data?.isTemp) &&
                applyPredicate(this.performerRegistrationDtRules, data?.registrationDt) &&
                applyPredicate(this.performerLanguageGroupsRules, data?.languageGroups) &&
                applyPredicate(this.performerLanguagePrimaryRules, data?.languagePrimary)
    }

    companion object {
        private fun <T> applyPredicate(rules: Predicate<T>?, value: T?): Boolean {
            if (rules == null) return true
            if (value == null) return false
            return rules.test(value)
        }
    }
}
