package org.wikipedia.history.db

import androidx.room.*
import org.wikipedia.history.HistoryEntry

@Dao
interface HistoryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HistoryEntry)

    @Query("SELECT * FROM HistoryEntry WHERE UPPER(displayTitle) LIKE UPPER(:term) ESCAPE '\\'")
    suspend fun findEntryBySearchTerm(term: String): HistoryEntry?

    @Query("SELECT * FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND apiTitle = :apiTitle LIMIT 1")
    suspend fun findEntryBy(authority: String, lang: String, apiTitle: String): HistoryEntry?

    @Query("SELECT * FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND apiTitle = :apiTitle AND timestamp = :timestamp LIMIT 1")
    suspend fun findEntryBy(authority: String, lang: String, apiTitle: String, timestamp: Long): HistoryEntry?

    @Query("DELETE FROM HistoryEntry")
    suspend fun deleteAll()

    @Query("DELETE FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle")
    suspend fun deleteBy(authority: String, lang: String, namespace: String?, apiTitle: String)

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
    suspend fun upsert(entry: HistoryEntry) {
        val curEntry = findEntryBy(entry.authority, entry.lang, entry.apiTitle, entry.timestamp.time)
        if (curEntry != null) {
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
