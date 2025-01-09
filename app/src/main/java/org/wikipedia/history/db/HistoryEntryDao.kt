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
    suspend fun upsertWithTimeSpent(entry: HistoryEntry, timeSpent: Int) {
        val curEntry = findEntryBy(entry.authority, entry.lang, entry.apiTitle)
        if (curEntry != null) {
            curEntry.timeSpentSec += timeSpent
            curEntry.source = entry.source
            curEntry.timestamp = entry.timestamp
            curEntry.description = entry.description
            insertEntry(curEntry)
        } else {
            entry.timeSpentSec += timeSpent
            insertEntry(entry)
        }
    }
}
