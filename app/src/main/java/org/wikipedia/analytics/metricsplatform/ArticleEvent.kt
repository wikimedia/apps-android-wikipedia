package org.wikipedia.analytics.metricsplatform

import org.wikimedia.metrics_platform.context.PageData
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import java.util.concurrent.TimeUnit

class ArticleFindInPageInteraction(private val fragment: PageFragment) : TimedMetricsEvent() {
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
        submitEvent(
            "android.product_metrics.find_in_page_interaction",
            "/analytics/mobile_apps/product_metrics/android_find_in_page_interaction/1.1.1",
            "find_in_page_interaction",
            mapOf(
                "find_text" to findText,
                "find_next_clicks_count" to numFindNext,
                "find_prev_clicks_count" to numFindPrev,
                "page_height" to pageHeight,
                "time_spent_ms" to timer.elapsedMillis,
            ),
            getInteractionData("find_in_page_interaction"),
            getPageData(fragment)
        )
    }
}

class ArticleToolbarInteraction(private val fragment: PageFragment) : TimedMetricsEvent() {

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

        submitEvent(
            "android.product_metrics.article_toolbar_interaction",
            "article_toolbar_interaction",
            getInteractionData(
                action,
                null,
                null,
                "time_spent_ms.${timer.elapsedMillis}"
            ),
            getPageData(fragment)
        )
    }
}

class ArticleTocInteraction(private val fragment: PageFragment, private val numSections: Int) : MetricsEvent() {
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
        submitEvent(
            "android.product_metrics.article_toc_interaction",
            "/analytics/mobile_apps/product_metrics/android_article_toc_interaction/1.1.1",
            "article_toc_interaction",
            mapOf(
                "num_opens" to numOpens,
                "num_section_clicks" to numSectionClicks,
                "total_open_sec" to totalOpenedSec,
                "num_sections" to numSections
            ),
            getInteractionData("article_toc_interaction"),
            getPageData(fragment)
        )
    }
}

class ArticleLinkPreviewInteraction : TimedMetricsEvent {
    private val pageData: PageData?
    private val source: Int

    constructor(fragment: PageFragment, source: Int) {
        this.source = source
        pageData = getPageData(fragment)
    }

    constructor(pageTitle: PageTitle, pageId: Int, source: Int) {
        this.source = source
        pageData = getPageData(pageTitle, pageId)
    }

    constructor(pageTitle: PageTitle, summary: PageSummary, source: Int) {
        this.source = source
        pageData = getPageData(pageTitle, summary)
    }

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
        submitEvent(
            "android.product_metrics.article_link_preview_interaction",
            "article_link_preview_interaction",
            getInteractionData(
                action,
                null,
                source.toString(),
                "time_spent_ms.${timer.elapsedMillis}",
            ),
            pageData
        )
    }
}
