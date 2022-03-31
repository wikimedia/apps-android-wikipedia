package org.wikipedia.search.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.Instant

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(recentSearch: RecentSearch)

    @Query("SELECT * FROM RecentSearch ORDER BY timestamp DESC")
    suspend fun getRecentSearches(): List<RecentSearch>

    @Query("DELETE FROM RecentSearch")
    suspend fun deleteAll()

    @Query("DELETE FROM RecentSearch WHERE text = :text AND timestamp = :timestamp")
    suspend fun deleteBy(text: String, timestamp: Instant)

    suspend fun delete(recentSearch: RecentSearch) {
        deleteBy(recentSearch.text, recentSearch.timestamp)
    }
}
