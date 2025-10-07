package org.wikipedia.history.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wikipedia.history.HistoryEntry

@Dao
interface HistoryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HistoryEntry): Long

    @Query("SELECT * FROM HistoryEntry WHERE UPPER(displayTitle) LIKE UPPER(:term) ESCAPE '\\'")
    suspend fun findEntryBySearchTerm(term: String): HistoryEntry?

    @Query("SELECT * FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND apiTitle = :apiTitle LIMIT 1")
    suspend fun findEntryBy(authority: String, lang: String, apiTitle: String): HistoryEntry?

    @Query("SELECT * FROM HistoryEntry ORDER BY RANDOM() DESC LIMIT :limit")
    suspend fun getHistoryEntriesByRandom(limit: Int): List<HistoryEntry>

    @Query("SELECT * FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND apiTitle = :apiTitle AND timestamp = :timestamp LIMIT 1")
    suspend fun findEntryBy(authority: String, lang: String, apiTitle: String, timestamp: Long): HistoryEntry?

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT HistoryEntry.lang, HistoryEntry.apiTitle FROM HistoryEntry WHERE timestamp BETWEEN :startDate AND :endDate)")
    suspend fun getDistinctEntriesCountBetween(startDate: Long?, endDate: Long?): Int

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT HistoryEntry.lang, HistoryEntry.apiTitle FROM HistoryEntry WHERE timestamp > :timestamp)")
    suspend fun getDistinctEntriesCountSince(timestamp: Long): Int?

    @Query("SELECT displayTitle FROM HistoryEntry WHERE timestamp > :timestamp GROUP BY displayTitle ORDER BY COUNT(displayTitle) DESC LIMIT :limit")
    suspend fun getTopVisitedEntriesSince(limit: Int, timestamp: Long): List<String>

    @Query("SELECT COUNT(*) FROM HistoryEntry")
    suspend fun getHistoryCount(): Int

    @Query("DELETE FROM HistoryEntry")
    suspend fun deleteAll()

    @Query("DELETE FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle")
    suspend fun deleteBy(authority: String, lang: String, namespace: String?, apiTitle: String)

    @Query("SELECT * FROM HistoryEntry ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentEntry(): HistoryEntry?

    @Query("SELECT CAST(strftime('%H', timestamp / 1000, 'unixepoch') AS INTEGER) AS hour FROM HistoryEntry WHERE timestamp BETWEEN :startDate AND :endDate GROUP BY hour ORDER BY COUNT(id) DESC LIMIT 1")
    suspend fun getFavoriteTimeToReadSince(startDate: Long, endDate: Long): Int?

    @Query("SELECT CAST(strftime('%w', timestamp / 1000, 'unixepoch') AS INTEGER) AS dayOfWeek FROM HistoryEntry WHERE timestamp BETWEEN :startDate AND :endDate GROUP BY dayOfWeek ORDER BY COUNT(id) DESC LIMIT 1")
    suspend fun getFavoriteDayToReadSince(startDate: Long, endDate: Long): Int?

    @Query("SELECT CAST(strftime('%m', timestamp / 1000, 'unixepoch') AS INTEGER) AS month FROM HistoryEntry WHERE timestamp BETWEEN :startDate AND :endDate GROUP BY month ORDER BY COUNT(id) DESC LIMIT 1")
    suspend fun getMostReadingMonthSince(startDate: Long, endDate: Long): Int?

    @Transaction
    suspend fun insert(entries: List<HistoryEntry>) {
        entries.forEach {
            insertEntry(it)
        }
    }

    suspend fun delete(entry: HistoryEntry) {
        deleteBy(entry.authority, entry.lang, entry.namespace, entry.apiTitle)
    }

    @Transaction
    suspend fun upsert(entry: HistoryEntry): Long {
        val curEntry = findEntryBy(entry.authority, entry.lang, entry.apiTitle, entry.timestamp.time)
        return if (curEntry != null) {
            // If this entry already exists, it implies that the page was refreshed, so it's OK not to
            // create a new db entry. But just for good measure, lets update the displayTitle anyway,
            // since it might have changed.
            curEntry.displayTitle = entry.displayTitle
            insertEntry(curEntry)
        } else {
            // This is definitely a new visit to this page, so create a new db entry.
            insertEntry(entry)
        }
    }
}
