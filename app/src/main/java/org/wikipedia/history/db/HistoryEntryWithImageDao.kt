package org.wikipedia.history.db

import androidx.room.*
import org.apache.commons.lang3.StringUtils
import org.wikipedia.history.HistoryEntry
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import java.text.DateFormat
import java.util.*

@Dao
interface HistoryEntryWithImageDao {

    // TODO: convert to PagingSource.
    // https://developer.android.com/topic/libraries/architecture/paging/v3-overview
    @Query("SELECT HistoryEntry.*, PageImage.imageName FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) WHERE UPPER(HistoryEntry.displayTitle) LIKE UPPER(:term) ESCAPE '\\' ORDER BY timestamp DESC")
    @RewriteQueriesToDropUnusedColumns
    fun findEntriesBySearchTerm(term: String): List<HistoryEntryWithImage>

    // TODO: convert to PagingSource.
    @Query("SELECT HistoryEntry.*, PageImage.imageName FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) WHERE source != :excludeSource1 AND source != :excludeSource2 AND source != :excludeSource3 AND timeSpentSec >= :minTimeSpent ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    fun findEntriesBy(excludeSource1: Int, excludeSource2: Int, excludeSource3: Int, minTimeSpent: Int, limit: Int): List<HistoryEntryWithImage>

    fun findHistoryItem(searchQuery: String): SearchResults {
        var normalizedQuery = StringUtils.stripAccents(searchQuery).lowercase(Locale.getDefault())
        if (normalizedQuery.isEmpty()) {
            return SearchResults()
        }
        normalizedQuery = normalizedQuery.replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_")

        val entries = findEntriesBySearchTerm("%$normalizedQuery%")

        return if (entries.isEmpty()) SearchResults() else SearchResults(
            mutableListOf(SearchResult(toHistoryEntry(entries[0]).title, SearchResult.SearchResultType.HISTORY))
        )
    }

    fun filterHistoryItems(searchQuery: String): List<Any> {
        val list = mutableListOf<Any>()
        val normalizedQuery = StringUtils.stripAccents(searchQuery).lowercase(Locale.getDefault())
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

        val entries = findEntriesBySearchTerm("%$normalizedQuery%")

        for (i in entries.indices) {
            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it if this is the first item.
            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it if this is the first item.
            val curTime: String = getDateString(entries[i].timestamp)
            val prevTime: String
            if (i > 0) {
                prevTime = getDateString(entries[i - 1].timestamp)
                if (curTime != prevTime) {
                    list.add(curTime)
                }
            } else {
                list.add(curTime)
            }
            list.add(toHistoryEntry(entries[i]))
        }
        return list
    }

    fun findEntryForReadMore(age: Int, minTimeSpent: Int): List<HistoryEntry> {
        val entries = findEntriesBy(HistoryEntry.SOURCE_MAIN_PAGE, HistoryEntry.SOURCE_RANDOM,
            HistoryEntry.SOURCE_FEED_MAIN_PAGE, minTimeSpent, age + 1)
        return entries.map { toHistoryEntry(it) }
    }

    private fun toHistoryEntry(entryWithImage: HistoryEntryWithImage): HistoryEntry {
        val entry = HistoryEntry(entryWithImage.authority, entryWithImage.lang, entryWithImage.apiTitle,
            entryWithImage.displayTitle, 0, entryWithImage.namespace, entryWithImage.timestamp,
            entryWithImage.source, entryWithImage.timeSpentSec)
        entry.title.thumbUrl = entryWithImage.imageName
        return entry
    }

    private fun getDateString(date: Date): String {
        return DateFormat.getDateInstance().format(date)
    }
}
