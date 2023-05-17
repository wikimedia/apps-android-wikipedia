package org.wikipedia.analytics.metricsplatform

import org.wikipedia.BuildConfig
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.ReleaseUtil

object ApplicationData {

    val data get() = mapOf(
        "agent_flavor" to BuildConfig.FLAVOR + BuildConfig.BUILD_TYPE,
        "app_theme" to WikipediaApp.instance.currentTheme,
        "app_version" to WikipediaApp.instance.versionCode.toString(),
        "database" to WikipediaApp.instance.wikiSite.dbName(),
        "device_language" to WikipediaApp.instance.languageState.systemLanguageCode,
        "is_prod" to ReleaseUtil.isProdRelease,
        "is_dev" to ReleaseUtil.isDevRelease,
        "language_groups" to WikipediaApp.instance.languageState.appLanguageCodes.toString(),
        "language_primary" to WikipediaApp.instance.languageState.appLanguageCode,
        "performer_id" to AccountUtil.hashCode().toString(),
        "performer_isloggedin" to AccountUtil.isLoggedIn,
        "performer_groups" to AccountUtil.groups,
        "performer_pageviewid" to EventPlatformClient.AssociationController.pageViewId,
        "performer_sessionid" to EventPlatformClient.AssociationController.sessionId,
        "performer_username" to AccountUtil.userName,
    )
}
