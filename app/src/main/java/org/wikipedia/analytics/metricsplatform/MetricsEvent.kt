package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.context.ClientData
import org.wikimedia.metrics_platform.context.PageData
import org.wikimedia.metrics_platform.context.PerformerData
import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.auth.AccountUtil
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ReleaseUtil

open class MetricsEvent {

    private val applicationData get() = mapOf(
        "agent_flavor" to BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE,
        "app_theme" to WikipediaApp.instance.currentTheme,
        "app_version" to WikipediaApp.instance.versionCode.toString(),
        "database" to WikipediaApp.instance.wikiSite.dbName(),
        "device_language" to WikipediaApp.instance.languageState.systemLanguageCode,
        "is_prod" to ReleaseUtil.isProdRelease,
        "is_dev" to ReleaseUtil.isDevRelease,
        "language_groups" to WikipediaApp.instance.languageState.appLanguageCodes.toString(),
        "language_primary" to WikipediaApp.instance.languageState.appLanguageCode,
    )

    protected fun submitEvent(eventName: String, customData: Map<String, Any>, pageData: PageData? = null) {
        if (ReleaseUtil.isPreBetaRelease && Prefs.isEventLoggingEnabled) {
            MetricsPlatform.client.submitMetricsEvent(
                EVENT_NAME_BASE + eventName,
                getClientData(pageData),
                customData + applicationData)
        }
    }

    private fun getClientData(pageData: PageData?): ClientData {
        return ClientData(
            MetricsPlatform.agentData,
            pageData,
            MetricsPlatform.mediawikiData,
            getPerformerData(),
            MetricsPlatform.domain
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
            fragment.model.title?.wikiSite?.languageCode.orEmpty(),
            null,
            null,
            null
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
            "",
            pageTitle.wikiSite.languageCode,
            null, null, null)
    }

    private fun getPerformerData(): PerformerData {
        return PerformerData(
            AccountUtil.userName,
            AccountUtil.isLoggedIn,
            AccountUtil.hashCode(),
            EventPlatformClient.AssociationController.sessionId,
            EventPlatformClient.AssociationController.pageViewId,
            AccountUtil.groups,
            null,
            WikipediaApp.instance.languageState.appLanguageCode,
            WikipediaApp.instance.languageState.appLanguageCodes.toString(),
            null,
            null,
            null,
            null
        )
    }

    companion object {
        private const val EVENT_NAME_BASE = "android.metrics_platform."
    }
}
