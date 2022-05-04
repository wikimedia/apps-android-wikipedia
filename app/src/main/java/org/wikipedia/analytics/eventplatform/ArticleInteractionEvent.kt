package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.auth.AccountUtil

class ArticleInteractionEvent(private val wikiDb: String, private val pageId: Int) : TimedEvent() {

    fun logLoaded() {
        submitEvent("load")
    }

    fun logSaveClick() {
        submitEvent("save")
    }

    fun logLanguageClick() {
        submitEvent("language")
    }

    fun logFindInArticleClick() {
        submitEvent("find_in_article")
    }

    fun logThemeClick() {
        submitEvent("theme")
    }

    fun logContentsClick() {
        submitEvent("contents")
    }

    fun logMoreClick() {
        submitEvent("more")
    }

    fun logShareClick() {
        submitEvent("share")
    }

    fun logTalkPageClick() {
        submitEvent("talk_page")
    }

    fun logEditHistoryClick() {
        submitEvent("edit_history")
    }

    fun logNewTabClick() {
        submitEvent("new_tab")
    }

    fun logExploreClick() {
        submitEvent("explore")
    }

    fun logForwardClick() {
        submitEvent("forward")
    }

    fun logNotificationClick() {
        submitEvent("notification")
    }

    fun logTabsClick() {
        submitEvent("tabs")
    }

    fun logSearchWikipediaClick() {
        submitEvent("search_wikipedia")
    }

    fun logBackClick() {
        submitEvent("back")
    }

    fun logEditHistoryArticleClick() {
        submitEvent("edit_history_from_article")
    }

    fun logTalkPageArticleClick() {
        submitEvent("talk_page_from_article")
    }

    fun logTocSwipe() {
        submitEvent("toc_swipe")
    }

    fun logCategoriesClick() {
        submitEvent("categories")
    }

    fun logWatchClick() {
        submitEvent("watch_article")
    }

    fun logUnWatchClick() {
        submitEvent("unwatch_article")
    }

    private fun submitEvent(action: String) {
        EventPlatformClient.submit(ArticleInteractionEventImpl(!AccountUtil.isLoggedIn, duration, wikiDb, pageId, action))
    }

    @Suppress("unused")
    @Serializable
    @SerialName("/analytics/mobile_apps/android_article_toolbar_interaction/1.0.0")
    class ArticleInteractionEventImpl(@SerialName("is_anon") private val isAnon: Boolean,
                                      @SerialName("time_spent_ms") private val timeSpentMs: Int,
                                      @SerialName("wiki_db") private val wikiDb: String,
                                      @SerialName("page_id") private val pageId: Int,
                                      private val action: String) :
        MobileAppsEvent("android.article_toolbar_interaction")
}
