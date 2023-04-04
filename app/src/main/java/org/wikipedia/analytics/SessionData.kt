package org.wikipedia.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.wikipedia.history.HistoryEntry
import org.wikipedia.util.MathUtil
import java.util.concurrent.TimeUnit

@Suppress("suspiciousVarProperty")
@Serializable
class SessionData {

    @Transient private val pageLatency = MathUtil.Averaged<Long>()
    @SerialName("total_pages") var totalPages: Int = 0
        get() = pagesFromSearch + pagesFromRandom + pagesFromLangLink + pagesFromInternal +
                pagesFromExternal + pagesFromHistory + pagesFromReadingList + pagesFromSuggestedEdits
    @Transient var startTime: Long = 0
    @Transient var lastTouchTime: Long = 0
    @SerialName("page_load_latency_ms") private var averagedPageLatency = 0
        get() = pageLatency.average.toInt()
    @SerialName("from_search") var pagesFromSearch = 0
    @SerialName("from_random") var pagesFromRandom = 0
    @SerialName("from_lang_link") var pagesFromLangLink = 0
    @SerialName("from_internal") var pagesFromInternal = 0
    @SerialName("from_external") var pagesFromExternal = 0
    @SerialName("from_history") var pagesFromHistory = 0
    @SerialName("from_reading_list") var pagesFromReadingList = 0
    @SerialName("from_back") var pagesFromBack = 0
    @SerialName("no_description") var pagesWithNoDescription = 0
    @SerialName("from_suggested_edits") var pagesFromSuggestedEdits = 0

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

    fun addPageLatency(pageLatency: Long) {
        this.pageLatency.addSample(TimeUnit.NANOSECONDS.toMillis(pageLatency))
    }

    fun addPageFromBack() {
        pagesFromBack++
    }

    fun addPageWithNoDescription() {
        pagesWithNoDescription++
    }
}
