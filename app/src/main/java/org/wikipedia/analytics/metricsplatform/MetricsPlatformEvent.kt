package org.wikipedia.analytics.metricsplatform

import java.util.Collections

class MetricsPlatformEvent {
    private var startTime = System.currentTimeMillis()
    private var pauseTime = 0L

    val duration get() = (System.currentTimeMillis() - startTime).toInt()

    fun pause() {
        pauseTime = System.currentTimeMillis()
    }

    fun resume() {
        if (pauseTime > 0) {
            startTime += System.currentTimeMillis() - pauseTime
        }
        pauseTime = 0
    }

    fun reset() {
        startTime = System.currentTimeMillis()
    }

    inner class ArticleToolbarInteraction(private val wikiDb: String, private val pageId: Int) {

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

        fun logEditArticleClick() {
            submitEvent("edit_article")
        }

        private fun submitEvent(action: String) {
            submitEvent(action, "android.metrics_platform.article_toolbar_interaction")
        }
    }
    private fun submitEvent(action: String, eventName: String) {
        MetricsPlatformClient.client.submitMetricsEvent(eventName,
            Collections.singletonMap("action", action) as Map<String, Any>?
        )
    }
}
