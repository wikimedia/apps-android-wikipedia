package org.wikimedia.metrics_platform.config;

import java.time.Instant;
import java.util.function.Predicate;

import org.wikimedia.metrics_platform.config.curation.CollectionCurationRules;
import org.wikimedia.metrics_platform.config.curation.ComparableCurationRules;
import org.wikimedia.metrics_platform.config.curation.CurationRules;
import org.wikimedia.metrics_platform.context.AgentData;
import org.wikimedia.metrics_platform.context.MediawikiData;
import org.wikimedia.metrics_platform.context.PageData;
import org.wikimedia.metrics_platform.context.PerformerData;
import org.wikimedia.metrics_platform.event.EventProcessed;

import com.google.gson.annotations.SerializedName;

public class CurationFilter implements Predicate<EventProcessed> {
    @SerializedName("agent_app_install_id") CurationRules<String> agentAppInstallIdRules;
    @SerializedName("agent_client_platform") CurationRules<String> agentClientPlatformRules;
    @SerializedName("agent_client_platform_family") CurationRules<String> agentClientPlatformFamilyRules;

    @SerializedName("mediawiki_database") CurationRules<String> mediawikiDatabase;

    @SerializedName("page_id") ComparableCurationRules<Integer> pageIdRules;
    @SerializedName("page_namespace_id") ComparableCurationRules<Integer> pageNamespaceIdRules;
    @SerializedName("page_namespace_name") CurationRules<String> pageNamespaceNameRules;
    @SerializedName("page_title") CurationRules<String> pageTitleRules;
    @SerializedName("page_revision_id") ComparableCurationRules<Long> pageRevisionIdRules;
    @SerializedName("page_wikidata_qid") CurationRules<String> pageWikidataQidRules;
    @SerializedName("page_content_language") CurationRules<String> pageContentLanguageRules;

    @SerializedName("performer_id") ComparableCurationRules<Integer> performerIdRules;
    @SerializedName("performer_name") CurationRules<String> performerNameRules;
    @SerializedName("performer_session_id") CurationRules<String> performerSessionIdRules;
    @SerializedName("performer_pageview_id") CurationRules<String> performerPageviewIdRules;
    @SerializedName("performer_groups") CollectionCurationRules<String> performerGroupsRules;
    @SerializedName("performer_is_logged_in") CurationRules<Boolean> performerIsLoggedInRules;
    @SerializedName("performer_is_temp") CurationRules<Boolean> performerIsTempRules;
    @SerializedName("performer_registration_dt") ComparableCurationRules<Instant> performerRegistrationDtRules;
    @SerializedName("performer_language_groups") CurationRules<String> performerLanguageGroupsRules;
    @SerializedName("performer_language_primary") CurationRules<String> performerLanguagePrimaryRules;

    public boolean test(EventProcessed event) {
        return applyAgentRules(event.getAgentData())
                && applyMediaWikiRules(event.getMediawikiData())
                && applyPageRules(event.getPageData())
                && applyPerformerRules(event.getPerformerData());
    }

    private boolean applyAgentRules(@Nonnull AgentData data) {
        return applyPredicate(this.agentAppInstallIdRules, data.getAppInstallId())
                && applyPredicate(this.agentClientPlatformRules, data.getClientPlatform())
                && applyPredicate(this.agentClientPlatformFamilyRules, data.getClientPlatformFamily());
    }

    private boolean applyMediaWikiRules(@Nonnull MediawikiData data) {
        return applyPredicate(this.mediawikiDatabase, data.getDatabase());
    }

    private boolean applyPageRules(@Nonnull PageData data) {
        return applyPredicate(this.pageIdRules, data.getId())
                && applyPredicate(this.pageNamespaceIdRules, data.getNamespaceId())
                && applyPredicate(this.pageNamespaceNameRules, data.getNamespaceName())
                && applyPredicate(this.pageTitleRules, data.getTitle())
                && applyPredicate(this.pageRevisionIdRules, data.getRevisionId())
                && applyPredicate(this.pageWikidataQidRules, data.getWikidataItemQid())
                && applyPredicate(this.pageContentLanguageRules, data.getContentLanguage());
    }

    private boolean applyPerformerRules(@Nonnull PerformerData data) {
        return applyPredicate(this.performerIdRules, data.getId())
                && applyPredicate(this.performerNameRules, data.getName())
                && applyPredicate(this.performerSessionIdRules, data.getSessionId())
                && applyPredicate(this.performerPageviewIdRules, data.getPageviewId())
                && applyPredicate(this.performerGroupsRules, data.getGroups())
                && applyPredicate(this.performerIsLoggedInRules, data.getIsLoggedIn())
                && applyPredicate(this.performerIsTempRules, data.getIsTemp())
                && applyPredicate(this.performerRegistrationDtRules, data.getRegistrationDt())
                && applyPredicate(this.performerLanguageGroupsRules, data.getLanguageGroups())
                && applyPredicate(this.performerLanguagePrimaryRules, data.getLanguagePrimary());
    }

    private static <T> boolean applyPredicate(@Nullable Predicate<T> rules, @Nullable T value) {
        return rules == null || rules.test(value);
    }
}
