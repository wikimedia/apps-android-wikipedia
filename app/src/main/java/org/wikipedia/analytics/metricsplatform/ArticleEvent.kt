package org.wikipedia.analytics.metricsplatform

import org.wikipedia.page.PageFragment
import java.util.concurrent.TimeUnit

class ArticleEvent : Event() {
    inner class ArticleFindInPageInteraction(private val fragment: PageFragment) {
        private var numFindNext = 0
        private var numFindPrev = 0
        var pageHeight = 0
        var findText = ""

        fun addFindNext() {
            numFindNext++
        }

        fun addFindPrev() {
            numFindPrev++
        }

        fun logDone() {
            val customData = mapOf(
                "find_text" to findText,
                "find_next_clicks_count" to numFindNext,
                "find_prev_clicks_count" to numFindPrev,
                "page_height" to pageHeight,
                "time_spent_ms" to duration,
            )

            submitEvent(
                "android.metrics_platform.find_in_page_interaction",
                fragment,
                customData
            )
        }
    }

    inner class ArticleToolbarInteraction(private val fragment: PageFragment) {

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
            val customData = mapOf(
                "action" to action,
                "time_spent_ms" to duration
            )
            submitEvent(
                "android.metrics_platform.article_toolbar_interaction",
                fragment,
                customData
            )
        }
    }

    inner class ArticleTocInteraction(
        private val fragment: PageFragment,
        private val numSections: Int
    ) {
        private var numOpens = 0
        private var numSectionClicks = 0
        private var lastScrollStartMillis = 0L
        private var totalOpenedSec = 0

        fun scrollStart() {
            numOpens++
            lastScrollStartMillis = System.currentTimeMillis()
        }

        fun scrollStop() {
            if (lastScrollStartMillis == 0L) {
                return
            }
            totalOpenedSec += ((System.currentTimeMillis() - lastScrollStartMillis) / TimeUnit.SECONDS.toMillis(1)).toInt()
            lastScrollStartMillis = 0
        }

        fun logClick() {
            numSectionClicks++
        }

        fun logEvent() {
            scrollStop()
            if (numSections == 0 || numOpens == 0) {
                return
            }

            val customData = mapOf(
                "num_opens" to numOpens,
                "num_section_clicks" to numSectionClicks,
                "total_open_sec" to totalOpenedSec,
                "num_sections" to numSections
            )
            submitEvent(
                "android.metrics_platform.article_toc_interaction",
                fragment,
                customData
            )
        }
    }

    private fun submitEvent(
        eventName: String,
        fragment: PageFragment,
        customData: Map<String, Any>
    ) {
        val clientMetadata = AndroidPageClientMetadata(fragment)
        val metricsPlatformClient = MetricsPlatformClientPage(clientMetadata)

        metricsPlatformClient.client.submitMetricsEvent(eventName, customData)
    }
}
