package org.wikipedia.analytics

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.MathUtil

@Serializable
class SessionData {

    @Transient private val leadLatency = MathUtil.Averaged<Long>()
    var startTime: Long
    var lastTouchTime: Long
    var pagesFromSearch = 0
    var pagesFromRandom = 0
    var pagesFromLangLink = 0
    var pagesFromInternal = 0
    var pagesFromExternal = 0
    var pagesFromHistory = 0
    var pagesFromReadingList = 0
    var pagesFromBack = 0
    var pagesWithNoDescription = 0
    var pagesFromSuggestedEdits = 0

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
        get() = pagesFromSearch + pagesFromRandom + pagesFromLangLink + pagesFromInternal +
                pagesFromExternal + pagesFromHistory + pagesFromReadingList +
                pagesFromSuggestedEdits
}
