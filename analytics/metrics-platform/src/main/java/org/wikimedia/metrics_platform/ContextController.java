package org.wikimedia.metrics_platform;

import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_APP_INSTALL_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_CLIENT_PLATFORM;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_CLIENT_PLATFORM_FAMILY;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_APP_FLAVOR;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_APP_THEME;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_APP_VERSION;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_APP_VERSION_NAME;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_DEVICE_FAMILY;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_DEVICE_LANGUAGE;
import static org.wikimedia.metrics_platform.context.ContextValue.AGENT_RELEASE_STATUS;
import static org.wikimedia.metrics_platform.context.ContextValue.MEDIAWIKI_DATABASE;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_TITLE;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_NAMESPACE_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_NAMESPACE_NAME;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_REVISION_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_WIKIDATA_QID;
import static org.wikimedia.metrics_platform.context.ContextValue.PAGE_CONTENT_LANGUAGE;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_NAME;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_IS_LOGGED_IN;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_IS_TEMP;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_SESSION_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_PAGEVIEW_ID;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_GROUPS;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_LANGUAGE_GROUPS;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_LANGUAGE_PRIMARY;
import static org.wikimedia.metrics_platform.context.ContextValue.PERFORMER_REGISTRATION_DT;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.ThreadSafe;

import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.context.AgentData;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.MediawikiData;
import org.wikimedia.metrics_platform.context.PageData;
import org.wikimedia.metrics_platform.context.PerformerData;
import org.wikimedia.metrics_platform.event.EventProcessed;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ThreadSafe
@ParametersAreNonnullByDefault
public class ContextController {

    /**
     * @see <a href="https://wikitech.wikimedia.org/wiki/Metrics_Platform/Contextual_attributes">Metrics Platform/Contextual attributes</a>
     */
    private static final Collection<String> REQUIRED_PROPERTIES = List.of(
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
    );

    public void enrichEvent(EventProcessed event, StreamConfig streamConfig) {
        if (!streamConfig.hasRequestedContextValuesConfig()) {
            return;
        }
        // Check stream config for which contextual values should be added to the event.
        Collection<String> requestedValuesFromConfig = streamConfig.getProducerConfig()
                .getMetricsPlatformClientConfig().getRequestedValues();
        // Add required properties.
        Set<String> requestedValues = new HashSet<>(requestedValuesFromConfig);
        requestedValues.addAll(REQUIRED_PROPERTIES);
        ClientData filteredData = filterClientData(event.getClientData(), requestedValues);
        event.setClientData(filteredData);
    }

    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    @SuppressFBWarnings(value = "CC_CYCLOMATIC_COMPLEXITY", justification = "TODO: needs to be refactored")
    private ClientData filterClientData(ClientData clientData, Collection<String> requestedValues) {
        AgentData.AgentDataBuilder agentBuilder = AgentData.builder();
        PageData.PageDataBuilder pageBuilder = PageData.builder();
        MediawikiData.MediawikiDataBuilder mediawikiBuilder = MediawikiData.builder();
        PerformerData.PerformerDataBuilder performerBuilder = PerformerData.builder();

        AgentData agentData = clientData.getAgentData();
        PageData pageData = clientData.getPageData();
        MediawikiData mediawikiData = clientData.getMediawikiData();
        PerformerData performerData = clientData.getPerformerData();

        for (String requestedValue : requestedValues) {
            switch (requestedValue) {
                case AGENT_APP_INSTALL_ID:
                    agentBuilder.appInstallId(agentData.getAppInstallId());
                    break;
                case AGENT_CLIENT_PLATFORM:
                    agentBuilder.clientPlatform(agentData.getClientPlatform());
                    break;
                case AGENT_CLIENT_PLATFORM_FAMILY:
                    agentBuilder.clientPlatformFamily(agentData.getClientPlatformFamily());
                    break;
                case AGENT_APP_FLAVOR:
                    agentBuilder.appFlavor(agentData.getAppFlavor());
                    break;
                case AGENT_APP_THEME:
                    agentBuilder.appTheme(agentData.getAppTheme());
                    break;
                case AGENT_APP_VERSION:
                    agentBuilder.appVersion(agentData.getAppVersion());
                    break;
                case AGENT_APP_VERSION_NAME:
                    agentBuilder.appVersionName(agentData.getAppVersionName());
                    break;
                case AGENT_DEVICE_FAMILY:
                    agentBuilder.deviceFamily(agentData.getDeviceFamily());
                    break;
                case AGENT_DEVICE_LANGUAGE:
                    agentBuilder.deviceLanguage(agentData.getDeviceLanguage());
                    break;
                case AGENT_RELEASE_STATUS:
                    agentBuilder.releaseStatus(agentData.getReleaseStatus());
                    break;
                case PAGE_ID:
                    pageBuilder.id(pageData.getId());
                    break;
                case PAGE_TITLE:
                    pageBuilder.title(pageData.getTitle());
                    break;
                case PAGE_NAMESPACE_ID:
                    pageBuilder.namespaceId(pageData.getNamespaceId());
                    break;
                case PAGE_NAMESPACE_NAME:
                    pageBuilder.namespaceName(pageData.getNamespaceName());
                    break;
                case PAGE_REVISION_ID:
                    pageBuilder.revisionId(pageData.getRevisionId());
                    break;
                case PAGE_WIKIDATA_QID:
                    pageBuilder.wikidataItemQid(pageData.getWikidataItemQid());
                    break;
                case PAGE_CONTENT_LANGUAGE:
                    pageBuilder.contentLanguage(pageData.getContentLanguage());
                    break;
                case MEDIAWIKI_DATABASE:
                    mediawikiBuilder.database(mediawikiData.getDatabase());
                    break;
                case PERFORMER_ID:
                    performerBuilder.id(performerData.getId());
                    break;
                case PERFORMER_NAME:
                    performerBuilder.name(performerData.getName());
                    break;
                case PERFORMER_IS_LOGGED_IN:
                    performerBuilder.isLoggedIn(performerData.getIsLoggedIn());
                    break;
                case PERFORMER_IS_TEMP:
                    performerBuilder.isTemp(performerData.getIsTemp());
                    break;
                case PERFORMER_SESSION_ID:
                    performerBuilder.sessionId(performerData.getSessionId());
                    break;
                case PERFORMER_PAGEVIEW_ID:
                    performerBuilder.pageviewId(performerData.getPageviewId());
                    break;
                case PERFORMER_GROUPS:
                    performerBuilder.groups(performerData.getGroups());
                    break;
                case PERFORMER_LANGUAGE_GROUPS:
                    var languageGroups = performerData.getLanguageGroups();
                    if (languageGroups != null && languageGroups.length > 255) {
                        languageGroups = languageGroups.substring(0, 255);
                    }
                    performerBuilder.languageGroups(languageGroups);
                    break;
                case PERFORMER_LANGUAGE_PRIMARY:
                    performerBuilder.languagePrimary(performerData.getLanguagePrimary());
                    break;
                case PERFORMER_REGISTRATION_DT:
                    performerBuilder.registrationDt(performerData.getRegistrationDt());
                    break;

                default:
                    throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property %s", requestedValue));
            }
        }

        ClientData.ClientDataBuilder clientDataBuilder = ClientData.builder();
        clientDataBuilder.agentData(agentBuilder.build());
        clientDataBuilder.pageData(pageBuilder.build());
        clientDataBuilder.mediawikiData(mediawikiBuilder.build());
        clientDataBuilder.performerData(performerBuilder.build());
        return clientDataBuilder.build();
    }
}
