package org.wikimedia.metricsplatform.context

/**
 * @see [Metrics Platform/Contextual attributes](https://wikitech.wikimedia.org/wiki/Metrics_Platform/Contextual_attributes)
 */
object ContextValue {
    const val AGENT_APP_INSTALL_ID: String = "agent_app_install_id"
    const val AGENT_CLIENT_PLATFORM: String = "agent_client_platform"
    const val AGENT_CLIENT_PLATFORM_FAMILY: String = "agent_client_platform_family"
    const val AGENT_APP_FLAVOR: String = "agent_app_flavor"
    const val AGENT_APP_THEME: String = "agent_app_theme"
    const val AGENT_APP_VERSION: String = "agent_app_version"
    const val AGENT_APP_VERSION_NAME: String = "agent_app_version_name"
    const val AGENT_DEVICE_FAMILY: String = "agent_device_family"
    const val AGENT_DEVICE_LANGUAGE: String = "agent_device_language"
    const val AGENT_RELEASE_STATUS: String = "agent_release_status"

    const val MEDIAWIKI_DATABASE: String = "mediawiki_database"

    const val PAGE_ID: String = "page_id"
    const val PAGE_TITLE: String = "page_title"
    const val PAGE_NAMESPACE_ID: String = "page_namespace_id"
    const val PAGE_NAMESPACE_NAME: String = "page_namespace_name"
    const val PAGE_REVISION_ID: String = "page_revision_id"
    const val PAGE_WIKIDATA_QID: String = "page_wikidata_qid"
    const val PAGE_CONTENT_LANGUAGE: String = "page_content_language"

    const val PERFORMER_ID: String = "performer_id"
    const val PERFORMER_NAME: String = "performer_name"
    const val PERFORMER_IS_LOGGED_IN: String = "performer_is_logged_in"
    const val PERFORMER_IS_TEMP: String = "performer_is_temp"
    const val PERFORMER_SESSION_ID: String = "performer_session_id"
    const val PERFORMER_PAGEVIEW_ID: String = "performer_pageview_id"
    const val PERFORMER_GROUPS: String = "performer_groups"
    const val PERFORMER_LANGUAGE_GROUPS: String = "performer_language_groups"
    const val PERFORMER_LANGUAGE_PRIMARY: String = "performer_language_primary"
    const val PERFORMER_REGISTRATION_DT: String = "performer_registration_dt"
}
