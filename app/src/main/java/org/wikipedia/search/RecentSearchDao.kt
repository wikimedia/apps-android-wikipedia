package org.wikipedia.search

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentSearchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRecentSearch(recentSearch: RecentSearch)

    @Query("SELECT * FROM recentsearches")
    fun getRecentSearches(): List<RecentSearch>

}
