package org.wikipedia.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Single

@Dao
interface RecentSearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentSearch(recentSearch: RecentSearch): Single<Unit>

    @Query("SELECT * FROM recentsearches ORDER BY timestamp DESC")
    fun getRecentSearches(): Single<List<RecentSearch>>

    @Query("DELETE FROM recentsearches")
    fun deleteAll(): Single<Unit>
}
