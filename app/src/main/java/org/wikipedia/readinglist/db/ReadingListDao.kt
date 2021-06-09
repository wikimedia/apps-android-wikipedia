package org.wikipedia.readinglist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Single
import org.wikipedia.readinglist.database.ReadingList

@Dao
interface ReadingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReadingList(list: ReadingList)

    @Query("SELECT * FROM localreadinglist ORDER BY timestamp DESC")
    fun getRecentSearches(): Single<List<ReadingList>>
}
