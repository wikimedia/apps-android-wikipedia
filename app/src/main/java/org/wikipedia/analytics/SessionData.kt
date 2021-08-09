package org.wikipedia.analytics

import com.squareup.moshi.JsonClass
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.MathUtil

@JsonClass(generateAdapter = true)
class SessionData(
    var startTime: Long = System.currentTimeMillis(),
    var lastTouchTime: Long = System.currentTimeMillis(),
    var pagesFromSearch: Int = 0,
    var pagesFromRandom: Int = 0,
    var pagesFromLangLink: Int = 0,
    var pagesFromInternal: Int = 0,
    var pagesFromExternal: Int = 0,
    var pagesFromHistory: Int = 0,
    var pagesFromReadingList: Int = 0,
    var pagesFromBack: Int = 0,
    var pagesWithNoDescription: Int = 0,
    var pagesFromSuggestedEdits: Int = 0
) {
    @Transient
    private val leadLatency = MathUtil.Averaged<Long>()

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
