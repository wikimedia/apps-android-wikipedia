package org.wikimedia.metricsplatform.config;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.wikimedia.metricsplatform.config.StreamConfigFetcher.METRICS_PLATFORM_SCHEMA_TITLE;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_APP_INSTALL_ID;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_APP_FLAVOR;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_APP_THEME;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_APP_VERSION;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_DEVICE_LANGUAGE;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_RELEASE_STATUS;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_CLIENT_PLATFORM;
import static org.wikimedia.metricsplatform.context.ContextValue.AGENT_CLIENT_PLATFORM_FAMILY;
import static org.wikimedia.metricsplatform.context.ContextValue.MEDIAWIKI_DATABASE;
import static org.wikimedia.metricsplatform.context.ContextValue.PAGE_TITLE;
import static org.wikimedia.metricsplatform.context.ContextValue.PAGE_ID;
import static org.wikimedia.metricsplatform.context.ContextValue.PAGE_NAMESPACE_ID;
import static org.wikimedia.metricsplatform.context.ContextValue.PAGE_WIKIDATA_QID;
import static org.wikimedia.metricsplatform.context.ContextValue.PERFORMER_SESSION_ID;
import static org.wikimedia.metricsplatform.context.ContextValue.PERFORMER_PAGEVIEW_ID;
import static org.wikimedia.metricsplatform.context.ContextValue.PERFORMER_LANGUAGE_GROUPS;
import static org.wikimedia.metricsplatform.context.ContextValue.PERFORMER_LANGUAGE_PRIMARY;
import static org.wikimedia.metricsplatform.curation.CurationFilterFixtures.curationFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.wikimedia.metricsplatform.config.sampling.SampleConfig;

public final class StreamConfigFixtures {

    public static final Map<String, StreamConfig> STREAM_CONFIGS_WITH_EVENTS = new HashMap<String, StreamConfig>() {{
            put("test.stream", sampleStreamConfig(true));
        }
    };

    private StreamConfigFixtures() {
        // Utility class, should never be instantiated
    }

    public static StreamConfig sampleStreamConfig(boolean hasEvents) {
        Set<String> emptyEvents = emptySet();
        Set<String> testEvents = new HashSet<>(singletonList("test.event"));
        Set<String> events = hasEvents ? testEvents : emptyEvents;
        Set<String> requestedValuesSet = new HashSet<>(Arrays.asList(
            "agent_app_install_id",
            "agent_client_platform",
            "agent_client_platform_family",
            "mediawiki_database",
            "page_id",
            "page_namespace_id",
            "page_namespace_name",
            "page_title",
            "page_revision_id",
            "page_content_language",
            "page_wikidata_qid",
            "performer_id",
            "performer_is_logged_in",
            "performer_is_temp",
            "performer_name",
            "performer_session_id",
            "performer_pageview_id",
            "performer_groups",
            "performer_registration_dt",
            "performer_language_groups",
            "performer_language_primary"
        ));
        SampleConfig sampleConfig = new SampleConfig(1.0, SampleConfig.Identifier.PAGEVIEW);

        return new StreamConfig(
            "test.stream",
            "test/event",
            DestinationEventService.ANALYTICS,
            new StreamConfig.ProducerConfig(
                new StreamConfig.MetricsPlatformClientConfig(
                    events,
                    requestedValuesSet,
                    CurationFilterFixtures.getCurationFilter()
                )
            ),
            sampleConfig
        );
    }

    /**
     * Convenience method for getting stream config.
     */
    public static StreamConfig streamConfig(CurationFilter curationFilter, String[] provideValues) {
        Set<String> events = Collections.singleton("test_event");
        SampleConfig sampleConfig = new SampleConfig(1.0f, SampleConfig.Identifier.PAGEVIEW);

        return new StreamConfig(
            "test_stream",
            METRICS_PLATFORM_SCHEMA_TITLE,
            DestinationEventService.LOCAL,
            new StreamConfig.ProducerConfig(
                new StreamConfig.MetricsPlatformClientConfig(
                    events,
                    new HashSet<>(List.of(provideValues)),
                    curationFilter
                )
            ),
            sampleConfig
        );
    }

    /**
     * Convenience method for getting stream config.
     */
    public static StreamConfig streamConfig(CurationFilter curationFilter) {
        return streamConfig(curationFilter, provideValuesMinimum());
    }

    /**
     * Convenience method for getting a stream config map.
     */
    public static Map<String, StreamConfig> streamConfigMap(String[] provideValues) {
        return streamConfigMap(curationFilter(), provideValues);
    }


    public static Map<String, StreamConfig> streamConfigMap(CurationFilter curationFilter, String[] provideValues) {
        StreamConfig streamConfig = streamConfig(curationFilter, provideValues);
        return singletonMap(streamConfig.getStreamName(), streamConfig);
    }

    public static String[] provideValuesMinimum() {
        return new String[]{
            AGENT_CLIENT_PLATFORM,
            AGENT_CLIENT_PLATFORM_FAMILY,
            PAGE_TITLE,
            MEDIAWIKI_DATABASE,
            PERFORMER_SESSION_ID
        };
    }

    public static String[] provideValuesExtended() {
        return new String[]{
            AGENT_APP_INSTALL_ID,
            AGENT_CLIENT_PLATFORM,
            AGENT_CLIENT_PLATFORM_FAMILY,
            AGENT_APP_FLAVOR,
            AGENT_APP_THEME,
            AGENT_APP_VERSION,
            AGENT_DEVICE_LANGUAGE,
            AGENT_RELEASE_STATUS,
            PAGE_ID,
            PAGE_TITLE,
            PAGE_NAMESPACE_ID,
            PAGE_WIKIDATA_QID,
            MEDIAWIKI_DATABASE,
            PERFORMER_SESSION_ID,
            PERFORMER_PAGEVIEW_ID,
            PERFORMER_LANGUAGE_GROUPS,
            PERFORMER_LANGUAGE_PRIMARY,
        };
    }
}
