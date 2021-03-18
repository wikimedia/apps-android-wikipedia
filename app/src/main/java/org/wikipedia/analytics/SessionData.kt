package org.wikipedia.analytics

import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.MathUtil.Averaged

class SessionData {
    private val leadLatency = Averaged<Long>()
    var startTime: Long
    var lastTouchTime: Long
    var pagesFromSearch = 0
        private set
    var pagesFromRandom = 0
        private set
    var pagesFromLangLink = 0
        private set
    var pagesFromInternal = 0
        private set
    var pagesFromExternal = 0
        private set
    var pagesFromHistory = 0
        private set
    var pagesFromReadingList = 0
        private set
    var pagesFromBack = 0
        private set
    var pagesWithNoDescription = 0
        private set
    var pagesFromSuggestedEdits = 0
        private set

    init {
        val now = System.currentTimeMillis()
        startTime = now
        lastTouchTime = now
    }

    fun addPageViewed(entry: HistoryEntry) {
        when (entry.source) {
            HistoryEntry.SOURCE_SEARCH -> pagesFromSearch++
            HistoryEntry.SOURCE_RANDOM -> pagesFromRandom++
            HistoryEntry.SOURCE_LANGUAGE_LINK -> pagesFromLangLink++
            HistoryEntry.SOURCE_EXTERNAL_LINK -> pagesFromExternal++
            HistoryEntry.SOURCE_HISTORY -> pagesFromHistory++
            HistoryEntry.SOURCE_READING_LIST -> pagesFromReadingList++
            HistoryEntry.SOURCE_SUGGESTED_EDITS -> pagesFromSuggestedEdits++
            else -> pagesFromInternal++
        }
    }

    fun getLeadLatency(): Long {
        return leadLatency.average.toLong()
    }

    fun addLeadLatency(leadLatency: Long) {
        this.leadLatency.addSample(leadLatency)
    }

    fun addPageFromBack() {
        pagesFromBack++
    }

    fun addPageWithNoDescription() {
        pagesWithNoDescription++
    }

    val totalPages: Int
        get() = (pagesFromSearch + pagesFromRandom + pagesFromLangLink + pagesFromInternal
                + pagesFromExternal + pagesFromHistory + pagesFromReadingList
                + pagesFromSuggestedEdits)

}
