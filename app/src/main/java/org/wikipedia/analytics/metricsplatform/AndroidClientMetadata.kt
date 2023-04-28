package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.ClientMetadata
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.eventplatform.EventPlatformClient
import org.wikipedia.auth.AccountUtil
import org.wikipedia.util.ReleaseUtil
import java.time.Instant

open class AndroidClientMetadata : ClientMetadata {
    override fun getAgentAppInstallId(): String {
        return WikipediaApp.instance.appInstallID
    }

    override fun getAgentClientPlatform(): String {
        return "mobile app"
    }

    override fun getAgentClientPlatformFamily(): String {
        return "android"
    }

    override fun getMediawikiSkin(): String {
        return ""
    }

    override fun getMediawikiVersion(): String {
        return WikipediaApp.instance.versionCode.toString()
    }

    override fun getMediawikiIsProduction(): Boolean {
        return ReleaseUtil.isProdRelease
    }

    override fun getMediawikiIsDebugMode(): Boolean {
        return ReleaseUtil.isDevRelease
    }

    override fun getMediawikiDatabase(): String {
        return ""
    }

    override fun getMediawikiSiteContentLanguage(): String {
        return "" // WikipediaApp.instance.languageState.appLanguageCode
    }

    override fun getMediawikiSiteContentLanguageVariant(): String {
        return "" // WikipediaApp.instance.languageState.appLanguageCodes.component2()
    }

    override fun getPageId(): Int {
        return EventPlatformClient.AssociationController.pageViewId.toInt()
    }

    override fun getPageNamespace(): Int? {
        return null
    }

    override fun getPageNamespaceName(): String {
        return ""
    }

    override fun getPageTitle(): String {
        return ""
    }

    override fun getPageIsRedirect(): Boolean? {
        return null
    }

    override fun getPageRevisionId(): Int? {
        return null
    }

    override fun getPageWikidataItemQid(): String {
        return ""
    }

    override fun getPageContentLanguage(): String {
        return ""
    }

    override fun getPageUserGroupsAllowedToEdit(): MutableCollection<String>? {
        return null
    }

    override fun getPageUserGroupsAllowedToMove(): MutableCollection<String>? {
        return null
    }

    override fun getPerformerId(): Int? {
        return null
    }

    override fun getPerformerName(): String? {
        return AccountUtil.userName
    }

    override fun getPerformerSessionId(): String {
        return EventPlatformClient.AssociationController.sessionId
    }

    override fun getPerformerPageviewId(): String {
        return ""
    }

    override fun getPerformerIsLoggedIn(): Boolean {
        return AccountUtil.isLoggedIn
    }

    override fun getPerformerGroups(): Set<String> {
        return AccountUtil.groups
    }

    override fun getPerformerIsBot(): Boolean? {
        return null
    }

    override fun getPerformerCanProbablyEditPage(): Boolean? {
        return null
    }

    override fun getPerformerEditCount(): Int? {
        return null
    }

    override fun getPerformerEditCountBucket(): String {
        return ""
    }

    override fun getPerformerRegistrationDt(): Instant? {
        return null
    }

    override fun getPerformerLanguage(): String {
        return WikipediaApp.instance.appOrSystemLanguageCode
    }

    override fun getPerformerLanguageVariant(): String {
        return ""
    }

    override fun getDomain(): String {
        return ""
    }
}
