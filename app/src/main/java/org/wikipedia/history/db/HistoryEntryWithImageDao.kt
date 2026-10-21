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
    // One row per mainspace article (its most recent visit in the range), excluding the given Main Pages, ordered by the time spent on it in the range.
    // Time spent is only kept per article, as a total across all of its visits, so the article is credited with the share of that total
    // that matches its share of visits in the range; ties go to the more recently visited.
    @Query("""
        SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec,
        CAST(PageImage.timeSpentSec * RangeVisits.visitCount * 1.0 / AllVisits.visitCount AS INTEGER) AS creditedTimeSpentSec
        FROM HistoryEntry
        INNER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)
        INNER JOIN (
            SELECT lang, namespace, apiTitle, MAX(timestamp) AS max_timestamp, COUNT(*) AS visitCount FROM HistoryEntry
            WHERE timestamp BETWEEN :startMillis AND :endMillis
            AND namespace = '' AND apiTitle NOT IN (:mainPageApiTitles)
            GROUP BY lang, namespace, apiTitle
        ) RangeVisits ON HistoryEntry.lang = RangeVisits.lang AND HistoryEntry.namespace = RangeVisits.namespace AND HistoryEntry.apiTitle = RangeVisits.apiTitle AND HistoryEntry.timestamp = RangeVisits.max_timestamp
        INNER JOIN (
            SELECT lang, namespace, apiTitle, COUNT(*) AS visitCount FROM HistoryEntry GROUP BY lang, namespace, apiTitle
        ) AllVisits ON HistoryEntry.lang = AllVisits.lang AND HistoryEntry.namespace = AllVisits.namespace AND HistoryEntry.apiTitle = AllVisits.apiTitle
        WHERE PageImage.timeSpentSec > 0
        ORDER BY PageImage.timeSpentSec * RangeVisits.visitCount * 1.0 / AllVisits.visitCount DESC, HistoryEntry.timestamp DESC
        LIMIT :limit
    """
    )
    @RewriteQueriesToDropUnusedColumns
    suspend fun getLongestReadEntriesBetween(startMillis: Long, endMillis: Long, mainPageApiTitles: List<String>, limit: Int): List<HistoryEntryWithTimeSpent>

    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) INNER JOIN (SELECT lang, apiTitle, MAX(timestamp) as max_timestamp FROM HistoryEntry GROUP BY lang, apiTitle) LatestEntries ON HistoryEntry.apiTitle = LatestEntries.apiTitle AND HistoryEntry.timestamp = LatestEntries.max_timestamp ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    @RewriteQueriesToDropUnusedColumns
    suspend fun getHistoryEntriesWithOffset(limit: Int, offset: Int): List<HistoryEntryWithImage>

    // TODO: convert to PagingSource.
    // https://developer.android.com/topic/libraries/architecture/paging/v3-overview
    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) INNER JOIN (SELECT lang, apiTitle, MAX(timestamp) as max_timestamp FROM HistoryEntry GROUP BY lang, apiTitle) LatestEntries ON HistoryEntry.apiTitle = LatestEntries.apiTitle AND HistoryEntry.timestamp = LatestEntries.max_timestamp WHERE UPPER(HistoryEntry.displayTitle) LIKE UPPER(:term) ESCAPE '\\' ORDER BY timestamp DESC")
    @RewriteQueriesToDropUnusedColumns
    suspend fun findEntriesBySearchTerm(term: String): List<HistoryEntryWithImage>

    // TODO: convert to PagingSource.
    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) WHERE source != :excludeSource1 AND source != :excludeSource2 AND source != :excludeSource3 AND timeSpentSec >= :minTimeSpent AND (:langCode IS NULL OR HistoryEntry.lang = :langCode) ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    suspend fun findEntriesBy(excludeSource1: Int, excludeSource2: Int, excludeSource3: Int, minTimeSpent: Int, limit: Int, langCode: String? = null): List<HistoryEntryWithImage>

    @Query("SELECT SUM(timeSpentSec) FROM (" +
            "  SELECT DISTINCT HistoryEntry.lang, HistoryEntry.apiTitle, PageImage.timeSpentSec FROM HistoryEntry" +
            "  LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)" +
            "  WHERE timestamp BETWEEN :startMillis AND :endMillis" +
            ")")
    suspend fun getTimeSpentBetween(startMillis: Long, endMillis: Long = System.currentTimeMillis()): Long

    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry" +
            "  LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)" +
            "  WHERE PageImage.geoLat IS NOT 0.0 AND PageImage.geoLon IS NOT 0.0 AND timestamp BETWEEN :startMillis AND :endMillis ORDER BY timestamp DESC LIMIT :limit")
    @RewriteQueriesToDropUnusedColumns
    suspend fun getEntriesWithCoordinates(limit: Int, startMillis: Long, endMillis: Long = System.currentTimeMillis()): List<HistoryEntryWithImage>

    @Query("""
        SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec 
        FROM HistoryEntry 
        LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang) 
        INNER JOIN (
            SELECT lang, namespace, apiTitle, MAX(timestamp) as max_timestamp FROM HistoryEntry
            WHERE timestamp >= :sinceMillis
            AND source != :excludeSource1 AND source != :excludeSource2 AND source != :excludeSource3
            GROUP BY lang, namespace, apiTitle
        ) LatestEntries ON HistoryEntry.lang = LatestEntries.lang AND HistoryEntry.namespace = LatestEntries.namespace AND HistoryEntry.apiTitle = LatestEntries.apiTitle AND HistoryEntry.timestamp = LatestEntries.max_timestamp
        WHERE PageImage.timeSpentSec >= :minTimeSpent AND (:langCode IS NULL OR HistoryEntry.lang = :langCode)
        ORDER BY RANDOM() LIMIT :limit
    """
    )
    @RewriteQueriesToDropUnusedColumns
    suspend fun getRandomRecentReadEntries(sinceMillis: Long, excludeSource1: Int, excludeSource2: Int, excludeSource3: Int, minTimeSpent: Int, limit: Int, langCode: String? = null): List<HistoryEntryWithImage>

    // One row per article (its most recent visit in the range), most-visited first; ties go to the more recently visited.
    // Only mainspace articles are counted, excluding the Main Page of the given language.
    @Query("""
        SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec, RepeatedEntries.visit_count AS visitCount
        FROM HistoryEntry
        LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)
        INNER JOIN (
            SELECT lang, namespace, apiTitle, MAX(timestamp) AS max_timestamp, COUNT(*) AS visit_count FROM HistoryEntry
            WHERE timestamp BETWEEN :startMillis AND :endMillis
            AND (namespace IS NULL OR namespace = '' OR namespace = 'MAIN')
            AND apiTitle NOT IN (:mainPageApiTitles)
            GROUP BY lang, namespace, apiTitle
            HAVING COUNT(*) > 1
        ) RepeatedEntries ON HistoryEntry.lang = RepeatedEntries.lang AND HistoryEntry.namespace = RepeatedEntries.namespace AND HistoryEntry.apiTitle = RepeatedEntries.apiTitle AND HistoryEntry.timestamp = RepeatedEntries.max_timestamp
        ORDER BY RepeatedEntries.visit_count DESC, HistoryEntry.timestamp DESC
        LIMIT :limit
    """
    )
    @RewriteQueriesToDropUnusedColumns
    suspend fun getEntriesVisitedMultipleTimesBetween(startMillis: Long, endMillis: Long, mainPageApiTitles: List<String>, limit: Int): List<HistoryEntryWithVisitCount>

    // Time spent is only kept per article, as a total across all of its visits, so each visit is credited with an equal share of that total.
    // The local day with the most time spent reading mainspace articles, excluding the given Main Pages; a tie goes to the more recent day.
    @Query("""
        SELECT date(HistoryEntry.timestamp / 1000, 'unixepoch', 'localtime') AS day,
        CAST(SUM(PageImage.timeSpentSec * 1.0 / AllVisits.visitCount) AS INTEGER) AS timeSpentSec
        FROM HistoryEntry
        INNER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)
        INNER JOIN (
            SELECT lang, namespace, apiTitle, COUNT(*) AS visitCount FROM HistoryEntry GROUP BY lang, namespace, apiTitle
        ) AllVisits ON HistoryEntry.lang = AllVisits.lang AND HistoryEntry.namespace = AllVisits.namespace AND HistoryEntry.apiTitle = AllVisits.apiTitle
        WHERE HistoryEntry.timestamp BETWEEN :startMillis AND :endMillis
        AND HistoryEntry.namespace = '' AND HistoryEntry.apiTitle NOT IN (:mainPageApiTitles)
        AND PageImage.timeSpentSec > 0
        GROUP BY day
        ORDER BY SUM(PageImage.timeSpentSec * 1.0 / AllVisits.visitCount) DESC, day DESC
        LIMIT 1
    """
    )
    suspend fun getBiggestReadingDayBetween(startMillis: Long, endMillis: Long, mainPageApiTitles: List<String>): DayTimeSpent?

    // One row per mainspace article visited on the given local day (yyyy-MM-dd), excluding the given Main Pages, ordered by
    // the time spent on it that day, credited the same way as in getBiggestReadingDayBetween; ties go to the more recently visited.
    @Query("""
        SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec
        FROM HistoryEntry
        LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)
        INNER JOIN (
            SELECT lang, namespace, apiTitle, MAX(timestamp) AS max_timestamp, COUNT(*) AS dayVisitCount FROM HistoryEntry
            WHERE timestamp BETWEEN :startMillis AND :endMillis
            AND date(timestamp / 1000, 'unixepoch', 'localtime') = :day
            AND namespace = '' AND apiTitle NOT IN (:mainPageApiTitles)
            GROUP BY lang, namespace, apiTitle
        ) DayVisits ON HistoryEntry.lang = DayVisits.lang AND HistoryEntry.namespace = DayVisits.namespace AND HistoryEntry.apiTitle = DayVisits.apiTitle AND HistoryEntry.timestamp = DayVisits.max_timestamp
        INNER JOIN (
            SELECT lang, namespace, apiTitle, COUNT(*) AS visitCount FROM HistoryEntry GROUP BY lang, namespace, apiTitle
        ) AllVisits ON HistoryEntry.lang = AllVisits.lang AND HistoryEntry.namespace = AllVisits.namespace AND HistoryEntry.apiTitle = AllVisits.apiTitle
        ORDER BY COALESCE(PageImage.timeSpentSec, 0) * DayVisits.dayVisitCount * 1.0 / AllVisits.visitCount DESC, HistoryEntry.timestamp DESC
        LIMIT :limit
    """
    )
    @RewriteQueriesToDropUnusedColumns
    suspend fun getLongestReadEntriesOnDay(day: String, startMillis: Long, endMillis: Long, mainPageApiTitles: List<String>, limit: Int): List<HistoryEntryWithImage>

    @Query("SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec FROM HistoryEntry" +
            "  LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)" +
            "  WHERE HistoryEntry.id IN (:ids)")
    @RewriteQueriesToDropUnusedColumns
    suspend fun getEntriesByIds(ids: List<Long>): List<HistoryEntryWithImage>

    // One row per mainspace article (its most recent visit in the range), in random order, excluding the given titles.
    @Query("""
        SELECT HistoryEntry.*, PageImage.imageName, PageImage.description, PageImage.geoLat, PageImage.geoLon, PageImage.timeSpentSec
        FROM HistoryEntry
        LEFT OUTER JOIN PageImage ON (HistoryEntry.namespace = PageImage.namespace AND HistoryEntry.apiTitle = PageImage.apiTitle AND HistoryEntry.lang = PageImage.lang)
        INNER JOIN (
            SELECT lang, namespace, apiTitle, MAX(timestamp) AS max_timestamp FROM HistoryEntry
            WHERE timestamp BETWEEN :startMillis AND :endMillis
            AND namespace = '' AND apiTitle NOT IN (:excludedTitles)
            GROUP BY lang, namespace, apiTitle
        ) LatestEntries ON HistoryEntry.lang = LatestEntries.lang AND HistoryEntry.namespace = LatestEntries.namespace AND HistoryEntry.apiTitle = LatestEntries.apiTitle AND HistoryEntry.timestamp = LatestEntries.max_timestamp
        ORDER BY RANDOM()
        LIMIT :limit
    """
    )
    @RewriteQueriesToDropUnusedColumns
    suspend fun getRandomArticleEntriesBetween(startMillis: Long, endMillis: Long, excludedTitles: List<String>, limit: Int): List<HistoryEntryWithImage>

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

    suspend fun getMostRecentEntriesWithImage(limit: Int): List<HistoryEntry> {
        return getHistoryEntriesWithOffset(limit, 0).map { toHistoryEntry(it) }
    }

    suspend fun findEntriesVisitedMultipleTimesBetween(startMillis: Long, endMillis: Long, excludedTitles: List<String>, limit: Int): List<Pair<HistoryEntry, Int>> {
        return getEntriesVisitedMultipleTimesBetween(startMillis, endMillis, excludedTitles, limit)
            .map { toHistoryEntry(it.entryWithImage) to it.visitCount }
    }

    suspend fun findLongestReadEntriesBetween(startMillis: Long, endMillis: Long, excludedTitles: List<String>, limit: Int): List<Pair<HistoryEntry, Int>> {
        return getLongestReadEntriesBetween(startMillis, endMillis, excludedTitles, limit)
            .map { toHistoryEntry(it.entryWithImage) to it.creditedTimeSpentSec }
    }

    suspend fun findLongestReadEntriesOnDay(day: String, startMillis: Long, endMillis: Long, excludedTitles: List<String>, limit: Int): List<HistoryEntry> {
        return getLongestReadEntriesOnDay(day, startMillis, endMillis, excludedTitles, limit).map { toHistoryEntry(it) }
    }

    suspend fun findEntriesByIds(ids: List<Long>): List<HistoryEntry> {
        return getEntriesByIds(ids).map { toHistoryEntry(it) }
    }

    suspend fun findRandomArticleEntriesBetween(startMillis: Long, endMillis: Long, excludedTitles: List<String>, limit: Int): List<HistoryEntry> {
        return getRandomArticleEntriesBetween(startMillis, endMillis, excludedTitles, limit).map { toHistoryEntry(it) }
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

    suspend fun findEntryForReadMore(limit: Int, minTimeSpent: Int, langCode: String? = null): List<HistoryEntry> {
        return findEntriesBy(HistoryEntry.SOURCE_MAIN_PAGE, HistoryEntry.SOURCE_RANDOM,
            HistoryEntry.SOURCE_FEED_MAIN_PAGE, minTimeSpent, limit, langCode).map { toHistoryEntry(it) }
    }

    suspend fun findRandomSeedEntriesForReadMore(
        limit: Int,
        minTimeSpent: Int,
        sinceMillis: Long,
        langCode: String? = null
    ): List<HistoryEntry> {
        return getRandomRecentReadEntries(
            sinceMillis = sinceMillis,
            excludeSource1 = HistoryEntry.SOURCE_MAIN_PAGE,
            excludeSource2 = HistoryEntry.SOURCE_RANDOM,
            excludeSource3 = HistoryEntry.SOURCE_FEED_MAIN_PAGE,
            minTimeSpent = minTimeSpent,
            limit = limit,
            langCode = langCode
        ).map { toHistoryEntry(it) }
    }

    suspend fun getHistoryItemWIthImage(searchTerm: String): List<HistoryEntryWithImage> {
        val normalizedQuery = StringUtils.stripAccents(searchTerm).replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_")
        return findEntriesBySearchTerm("%$normalizedQuery%")
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
            id = entryWithImage.id,
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
