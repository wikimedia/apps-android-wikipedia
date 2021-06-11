package org.wikipedia.history.db

import androidx.room.*
import io.reactivex.rxjava3.core.Single
import org.wikipedia.history.HistoryEntry

@Dao
interface HistoryEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEntry(entry: HistoryEntry)

    @Query("SELECT * FROM HistoryEntry WHERE UPPER(displayTitle) LIKE UPPER(:term) ESCAPE '\\'")
    fun findEntryBySearchTerm(term: String): HistoryEntry?

    @Query("SELECT * FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND apiTitle = :apiTitle LIMIT 1")
    fun findEntryBy(authority: String, lang: String, apiTitle: String): HistoryEntry?

    @Query("DELETE FROM HistoryEntry")
    fun deleteAll(): Single<Unit>

    @Query("DELETE FROM HistoryEntry WHERE authority = :authority AND lang = :lang AND namespace = :namespace AND apiTitle = :apiTitle")
    fun deleteBy(authority: String, lang: String, namespace: String?, apiTitle: String)

    @Transaction
    fun insert(entries: List<HistoryEntry>) {
        entries.forEach {
            insertEntry(it)
        }
    }

    fun delete(entry: HistoryEntry) {
        deleteBy(entry.authority, entry.lang, entry.namespace, entry.apiTitle)
    }

    @Transaction
    fun upsertWithTimeSpent(entry: HistoryEntry, timeSpent: Int) {
        val curEntry = findEntryBy(entry.authority, entry.lang, entry.apiTitle)
        if (curEntry != null) {
            curEntry.timeSpentSec += timeSpent
            curEntry.source = entry.source
            curEntry.timestamp = entry.timestamp
            insertEntry(curEntry)
        } else {
            entry.timeSpentSec += timeSpent
            insertEntry(entry)
        }
    }
}
