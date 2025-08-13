@file:UseSerializers(InstantSerializer::class)

package org.wikimedia.metricsplatform.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.wikimedia.metricsplatform.config.curation.CollectionCurationRules
import org.wikimedia.metricsplatform.config.curation.ComparableCurationRules
import org.wikimedia.metricsplatform.config.curation.CurationRules
import org.wikimedia.metricsplatform.context.AgentData
import org.wikimedia.metricsplatform.context.InstantSerializer
import org.wikimedia.metricsplatform.context.MediawikiData
import org.wikimedia.metricsplatform.context.PageData
import org.wikimedia.metricsplatform.context.PerformerData
import org.wikimedia.metricsplatform.event.EventProcessed
import java.time.Instant
import java.util.function.Predicate

@Serializable
class CurationFilter : Predicate<EventProcessed> {
    @SerialName("agent_app_install_id") var agentAppInstallIdRules: CurationRules<String>? = null
    @SerialName("agent_client_platform") var agentClientPlatformRules: CurationRules<String>? = null
    @SerialName("agent_client_platform_family") var agentClientPlatformFamilyRules: CurationRules<String>? = null
    @SerialName("mediawiki_database") var mediawikiDatabase: CurationRules<String>? = null
    @SerialName("page_id") var pageIdRules: ComparableCurationRules<Int>? = null
    @SerialName("page_namespace_id") var pageNamespaceIdRules: ComparableCurationRules<Int>? = null
    @SerialName("page_namespace_name") var pageNamespaceNameRules: CurationRules<String>? = null
    @SerialName("page_title") var pageTitleRules: CurationRules<String>? = null
    @SerialName("page_revision_id") var pageRevisionIdRules: ComparableCurationRules<Long>? = null
    @SerialName("page_wikidata_qid") var pageWikidataQidRules: CurationRules<String>? = null
    @SerialName("page_content_language") var pageContentLanguageRules: CurationRules<String>? = null
    @SerialName("performer_id") var performerIdRules: ComparableCurationRules<Int>? = null
    @SerialName("performer_name") var performerNameRules: CurationRules<String>? = null
    @SerialName("performer_session_id") var performerSessionIdRules: CurationRules<String>? = null
    @SerialName("performer_pageview_id") var performerPageviewIdRules: CurationRules<String>? = null
    @SerialName("performer_groups") var performerGroupsRules: CollectionCurationRules<String>? = null
    @SerialName("performer_is_logged_in") var performerIsLoggedInRules: CurationRules<Boolean>? = null
    @SerialName("performer_is_temp") var performerIsTempRules: CurationRules<Boolean>? = null
    @SerialName("performer_registration_dt") var performerRegistrationDtRules: ComparableCurationRules<Instant>? = null

    @SerialName("performer_language_groups")
    var performerLanguageGroupsRules: CurationRules<String>? = null

    @SerialName("performer_language_primary")
    var performerLanguagePrimaryRules: CurationRules<String>? = null

    override fun test(event: EventProcessed): Boolean {
        return applyAgentRules(event.agentData)
                && applyMediaWikiRules(event.mediawikiData)
                && applyPageRules(event.pageData)
                && applyPerformerRules(event.performerData)
    }

    private fun applyAgentRules(data: AgentData?): Boolean {
        return applyPredicate(this.agentAppInstallIdRules, data?.appInstallId)
                && applyPredicate(this.agentClientPlatformRules, data?.clientPlatform)
                && applyPredicate(this.agentClientPlatformFamilyRules, data?.clientPlatformFamily)
    }

    private fun applyMediaWikiRules(data: MediawikiData?): Boolean {
        return applyPredicate(this.mediawikiDatabase, data?.database)
    }

    private fun applyPageRules(data: PageData?): Boolean {
        return applyPredicate(this.pageIdRules, data?.id)
                && applyPredicate(this.pageNamespaceIdRules, data?.namespaceId)
                && applyPredicate(this.pageNamespaceNameRules, data?.namespaceName)
                && applyPredicate(this.pageTitleRules, data?.title)
                && applyPredicate(this.pageRevisionIdRules, data?.revisionId)
                && applyPredicate(this.pageWikidataQidRules, data?.wikidataItemQid)
                && applyPredicate(this.pageContentLanguageRules, data?.contentLanguage)
    }

    private fun applyPerformerRules(data: PerformerData?): Boolean {
        return applyPredicate(this.performerIdRules, data?.id)
                && applyPredicate(this.performerNameRules, data?.name)
                && applyPredicate(this.performerSessionIdRules, data?.sessionId)
                && applyPredicate(this.performerPageviewIdRules, data?.pageviewId)
                && applyPredicate(this.performerGroupsRules, data?.groups)
                && applyPredicate(this.performerIsLoggedInRules, data?.isLoggedIn)
                && applyPredicate(this.performerIsTempRules, data?.isTemp)
                && applyPredicate(this.performerRegistrationDtRules, data?.registrationDt)
                && applyPredicate(this.performerLanguageGroupsRules, data?.languageGroups)
                && applyPredicate(this.performerLanguagePrimaryRules, data?.languagePrimary)
    }

    companion object {
        private fun <T> applyPredicate(rules: Predicate<T>?, value: T?): Boolean {
            if (rules == null) return true
            if (value == null) return false
            return rules.test(value)
        }
    }
}
