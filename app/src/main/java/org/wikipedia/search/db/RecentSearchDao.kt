package org.wikipedia.search.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.time.Instant

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentSearch(recentSearch: RecentSearch): Completable

    @Query("SELECT * FROM RecentSearch ORDER BY timestamp DESC")
    fun getRecentSearches(): Single<List<RecentSearch>>

    @Query("DELETE FROM RecentSearch")
    fun deleteAll(): Completable

    @Query("DELETE FROM RecentSearch WHERE text = :text AND timestamp = :timestamp")
    fun deleteBy(text: String, timestamp: Instant)

    fun delete(recentSearch: RecentSearch) {
        deleteBy(recentSearch.text, recentSearch.timestamp)
    }
}
