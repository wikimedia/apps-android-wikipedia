package org.wikimedia.testkitchen.config

import org.wikimedia.testkitchen.config.sampling.SampleConfig
import org.wikimedia.testkitchen.context.ContextValue

object StreamConfigFixtures {

    val STREAM_CONFIGS_WITH_EVENTS = mapOf("test.stream" to sampleStreamConfig(true))

    fun sampleStreamConfig(hasEvents: Boolean): StreamConfig {
        val emptyEvents = listOf<String>()
        val testEvents = listOf("test.event")
        val events = if (hasEvents) testEvents else emptyEvents
        val requestedValuesSet = listOf(
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
        )

        return StreamConfig().also {
            it.streamName = "test.stream"
            it.schemaTitle = "schema/title"
            it.destinationEventService = DestinationEventService.ANALYTICS
            it.sampleConfig = SampleConfig(1.0, SampleConfig.UNIT_PAGEVIEW)
            it.producerConfig = StreamConfig.ProducerConfig().also { pc ->
                pc.metricsPlatformClientConfig = StreamConfig.MetricsPlatformClientConfig().also { mpc ->
                    mpc.events = events
                    mpc.requestedValues = requestedValuesSet
                    mpc.curationFilter = CurationFilterFixtures.curationFilter
                }
            }
        }
    }

    fun streamConfig(
        curationFilter: CurationFilter?,
        provideValues: List<String> = provideValuesMinimum()
    ): StreamConfig {
        val events = listOf("test_event")

        return StreamConfig().also {
            it.streamName = "test.stream"
            it.schemaTitle = "schema/title"
            it.destinationEventService = DestinationEventService.LOCAL
            it.sampleConfig = SampleConfig(1.0, SampleConfig.UNIT_PAGEVIEW)
            it.producerConfig = StreamConfig.ProducerConfig().also { pc ->
                pc.metricsPlatformClientConfig = StreamConfig.MetricsPlatformClientConfig().also { mpc ->
                    mpc.events = events
                    mpc.requestedValues = provideValues
                    mpc.curationFilter = curationFilter
                }
            }
        }
    }

    fun streamConfigMap(
        provideValues: List<String>,
        curationFilter: CurationFilter? = null,
    ): Map<String, StreamConfig> {
        val streamConfig = streamConfig(curationFilter, provideValues)
        return mapOf(streamConfig.streamName to streamConfig)
    }

    fun provideValuesMinimum(): List<String> {
        return listOf(
            ContextValue.AGENT_CLIENT_PLATFORM,
            ContextValue.AGENT_CLIENT_PLATFORM_FAMILY,
            ContextValue.PAGE_TITLE,
            ContextValue.MEDIAWIKI_DATABASE,
            ContextValue.PERFORMER_SESSION_ID
        )
    }

    fun provideValuesExtended(): List<String> {
        return listOf(
            ContextValue.AGENT_APP_INSTALL_ID,
            ContextValue.AGENT_CLIENT_PLATFORM,
            ContextValue.AGENT_CLIENT_PLATFORM_FAMILY,
            ContextValue.AGENT_APP_FLAVOR,
            ContextValue.AGENT_APP_THEME,
            ContextValue.AGENT_APP_VERSION,
            ContextValue.AGENT_DEVICE_LANGUAGE,
            ContextValue.AGENT_RELEASE_STATUS,
            ContextValue.PAGE_ID,
            ContextValue.PAGE_TITLE,
            ContextValue.PAGE_NAMESPACE_ID,
            ContextValue.PAGE_WIKIDATA_QID,
            ContextValue.MEDIAWIKI_DATABASE,
            ContextValue.PERFORMER_SESSION_ID,
            ContextValue.PERFORMER_PAGEVIEW_ID,
            ContextValue.PERFORMER_LANGUAGE_GROUPS,
            ContextValue.PERFORMER_LANGUAGE_PRIMARY,
        )
    }
}
