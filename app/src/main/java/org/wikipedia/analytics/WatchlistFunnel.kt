package org.wikipedia.analytics

import org.wikipedia.WikipediaApp
import org.wikipedia.util.StringUtil

class WatchlistFunnel : Funnel(WikipediaApp.instance, SCHEMA_NAME, REV_ID, SAMPLE_LOG_ALL) {

    private fun logAction(action: String?) {
        log("action", action)
    }

    fun logShowTooltipMore() {
        logAction("show_tooltip_more")
    }

    fun logShowTooltip() {
        logAction("show_tooltip")
    }

    fun logAddArticle() {
        logAction("add_article")
    }

    fun logAddExpiry() {
        logAction("add_expiry")
    }

    fun logAddSuccess() {
        logAction("add_success")
    }

    fun logViewWatchlist() {
        logAction("view_watchlist")
    }

    fun logOpenWatchlist() {
        logAction("open_watchlist")
    }

    fun logRemoveArticle() {
        logAction("remove_article")
    }

    fun logRemoveSuccess() {
        logAction("remove_success")
    }

    fun logChangeLanguage(languagesList: List<String>) {
        log(
                "action", "change_language",
                "languages", StringUtil.listToJsonArrayString(languagesList)
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppWatchlist"
        private const val REV_ID = 20936401
    }
}
