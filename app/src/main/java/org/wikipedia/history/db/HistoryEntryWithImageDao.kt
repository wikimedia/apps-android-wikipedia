package org.wikipedia.history.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import org.apache.commons.lang3.StringUtils
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.search.SearchResult
import org.wikipedia.search.SearchResults
import org.wikipedia.util.StringUtil
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Dao
interface HistoryEntryWithImageDao {

    // TODO: convert to PagingSource.
    // https://developer.android.com/topic/libraries/architecture/paging/v3-overview
    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) INNER JOIN(SELECT lang, apiTitle, MAX(timestamp) as max_timestamp FROM HistoryEntry GROUP BY lang, apiTitle) LatestEntries ON HistoryEntry.apiTitle = LatestEntries.apiTitle AND HistoryEntry.timestamp = LatestEntries.max_timestamp WHERE UPPER(HistoryEntry.displayTitle) LIKE UPPER(:term) ESCAPE '\\' ORDER BY timestamp DESC")
    @RewriteQueriesToDropUnusedColumns
    suspend fun findEntriesBySearchTerm(term: String): List<HistoryEntryWithImage>

    // TODO: convert to PagingSource.
    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) WHERE source != :excludeSource1 AND source != :excludeSource2 AND source != :excludeSource3 AND timeSpentSec >= :minTimeSpent ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    suspend fun findEntriesBy(excludeSource1: Int, excludeSource2: Int, excludeSource3: Int, minTimeSpent: Int, limit: Int): List<HistoryEntryWithImage>

    suspend fun findHistoryItem(wikiSite: WikiSite, searchQuery: String): SearchResults {
        var normalizedQuery = StringUtils.stripAccents(searchQuery)
        if (normalizedQuery.isEmpty()) {
            return SearchResults()
        }
        normalizedQuery = normalizedQuery.replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_")

        val entries = findEntriesBySearchTerm("%$normalizedQuery%")
                .filter { wikiSite.languageCode == it.lang && StringUtil.fromHtml(it.displayTitle).contains(normalizedQuery, true) }

        return if (entries.isEmpty()) SearchResults()
        else SearchResults(entries.take(3).map { SearchResult(toHistoryEntry(it).title, SearchResult.SearchResultType.HISTORY) }.toMutableList())
    }

    suspend fun filterHistoryItemsWithoutTime(searchQuery: String = ""): List<HistoryEntry> {
        return findEntriesBySearchTerm("%${normalizedQuery(searchQuery)}%").map { toHistoryEntry(it) }
    }

    suspend fun filterHistoryItems(searchQuery: String): List<Any> {
        val list = mutableListOf<Any>()
        val entries = findEntriesBySearchTerm("%${normalizedQuery(searchQuery)}%")
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        var prevDay = 0
        entries.forEach { entry ->
            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it if this is the first item.
            // Check the previous item, see if the times differ enough
            // If they do, display the section header.
            // Always do it if this is the first item.
            calendar.time = entry.timestamp
            val curDay = calendar[Calendar.YEAR] + calendar[Calendar.DAY_OF_YEAR]
            if (prevDay == 0 || curDay != prevDay) {
                list.add(getDateString(entry.timestamp))
            }
            prevDay = curDay
            list.add(toHistoryEntry(entry))
        }
        return list
    }

    suspend fun findEntryForReadMore(age: Int, minTimeSpent: Int): List<HistoryEntry> {
        val entries = findEntriesBy(HistoryEntry.SOURCE_MAIN_PAGE, HistoryEntry.SOURCE_RANDOM,
            HistoryEntry.SOURCE_FEED_MAIN_PAGE, minTimeSpent, age + 1)
        return entries.map { toHistoryEntry(it) }
    }

    private fun normalizedQuery(searchQuery: String): String {
        return StringUtils.stripAccents(searchQuery).lowercase(Locale.getDefault())
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    private fun toHistoryEntry(entryWithImage: HistoryEntryWithImage): HistoryEntry {
        val entry = HistoryEntry(
            authority = entryWithImage.authority,
            lang = entryWithImage.lang,
            apiTitle = entryWithImage.apiTitle,
            displayTitle = entryWithImage.displayTitle,
            id = 0,
            namespace = entryWithImage.namespace,
            timestamp = entryWithImage.timestamp,
            source = entryWithImage.source
        )
        entry.title.thumbUrl = entryWithImage.imageName
        entry.title.description = entryWithImage.description

        return entry
    }

    private fun getDateString(date: Date): String {
        return DateFormat.getDateInstance().format(date)
    }
}
