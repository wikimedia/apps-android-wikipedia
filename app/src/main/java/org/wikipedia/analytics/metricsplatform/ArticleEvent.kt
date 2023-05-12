package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.context.PageData
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.ActiveTimer
import java.util.concurrent.TimeUnit

class ArticleEvent {
    val timer = ActiveTimer()

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
                "time_spent_ms" to timer.elapsedMillis,
            )

            submitEvent(
                "android.metrics_platform.find_in_page_interaction",
                getPageData(fragment),
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

        fun pause() { timer.pause() }
        fun resume() { timer.resume() }
        fun reset() { timer.reset() }

        private fun submitEvent(action: String) {
            val customData = mapOf(
                "action" to action,
                "time_spent_ms" to timer.elapsedMillis
            )
            submitEvent(
                "android.metrics_platform.article_toolbar_interaction",
                getPageData(fragment),
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
                getPageData(fragment),
                customData
            )
        }
    }

    inner class ArticleLinkPreviewInteraction(
        private val fragment: PageFragment,
        private val source: Int
    ) {
        private val PROD_LINK_PREVIEW_VERSION = 3
        fun logLinkClick() {
            submitEvent("linkclick")
        }

        fun logNavigate() {
            submitEvent(if (Prefs.isLinkPreviewEnabled) "navigate" else "disabled")
        }

        fun logCancel() {
            submitEvent("cancel")
        }

        private fun submitEvent(action: String) {
            val customData = mapOf(
                "action" to action,
                "source" to source,
                "time_spent_ms" to timer.elapsedMillis,
                "wiki_db" to (fragment.title?.wikiSite?.dbName() ?: ""),
                "version" to PROD_LINK_PREVIEW_VERSION
            )
            submitEvent(
                "android.metrics_platform.article_link_preview_interaction",
                getPageData(fragment),
                customData
            )
        }
    }

    inner class ArticleLinkPreviewDialogInteraction(
        private val wikiDb: String,
        private val pageId: Int,
        private val source: Int
    ) {
        private val PROD_LINK_PREVIEW_VERSION = 3
        fun logLinkClick() {
            submitEvent("linkclick")
        }

        fun logNavigate() {
            submitEvent(if (Prefs.isLinkPreviewEnabled) "navigate" else "disabled")
        }

        fun logCancel() {
            submitEvent("cancel")
        }

        private fun submitEvent(action: String) {
            val customData = mapOf(
                "action" to action,
                "source" to source,
                "time_spent_ms" to timer.elapsedMillis,
                "wiki_db" to wikiDb,
                "page_id" to pageId,
                "version" to PROD_LINK_PREVIEW_VERSION
            )
            submitEvent(
                "android.metrics_platform.article_link_preview_interaction",
                null,
                customData
            )
        }
    }

    /**
     * Submits events to the Metrics Platform.
     */
    private fun submitEvent(
        eventName: String,
        pageData: PageData?,
        customData: Map<String, Any>
    ) {
        MetricsPlatform.client.submitMetricsEvent(eventName, pageData, mergeData(customData))
    }

    private fun getPageData(fragment: PageFragment): PageData {
        val pageProperties = fragment.page?.pageProperties
        return PageData(
            pageProperties?.pageId ?: 0,
            pageProperties?.displayTitle ?: "",
            pageProperties?.namespace?.code() ?: 0,
            Namespace.of(pageProperties?.namespace?.code()!!).toString() ?: "",
            pageProperties.revisionId.toInt() ?: 0,
            pageProperties.wikiBaseItem ?: "",
            fragment.model.title?.wikiSite?.languageCode ?: "",
            null,
            null,
            null
        )
    }

    /**
     * Merges custom data with additional client metadata.
     */
    private fun mergeData(data: Map<String, Any>): Map<String, Any> {
        return data + ApplicationData.getApplicationData()
    }

    /**
     * Creates a new read-only map by replacing or adding entries to this map from another map.
     *
     * The returned map preserves the entry iteration order of the original map.
     * Those entries of another map that are missing in this map are iterated
     * in the end in the order of that map.
     */
    public operator fun <K, V> Map<out K, V>.plus(map: Map<out K, V>): Map<K, V> =
        LinkedHashMap(this).apply { putAll(map) }
}
