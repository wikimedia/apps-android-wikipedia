package org.wikipedia.yearinreview.data

import org.wikipedia.settings.Prefs

interface YearInReviewCache {
    fun get(year: Int): YearInReviewCachedStats?
    fun put(year: Int, stats: YearInReviewCachedStats)
}

object PrefsYearInReviewStore : YearInReviewCache {
    override fun get(year: Int) = Prefs.yearInReviewCachedStats[year]

    override fun put(year: Int, stats: YearInReviewCachedStats) {
        Prefs.yearInReviewCachedStats += year to stats
    }

    fun onReadingHistoryCleared() {
        Prefs.yearInReviewCachedStats = Prefs.yearInReviewCachedStats.mapValues { (_, stats) ->
            stats.copy(readingStats = null)
        }
    }

    fun onLoggedOut() {
        Prefs.yearInReviewCachedStats = Prefs.yearInReviewCachedStats.mapValues { (_, stats) ->
            stats.copy(editingStats = null)
        }
    }

    fun clearAll() {
        Prefs.yearInReviewCachedStats = emptyMap()
    }
}
