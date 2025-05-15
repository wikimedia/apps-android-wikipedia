package org.wikipedia.readinglist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.wikipedia.readinglist.database.RecommendedPage

@Dao
interface RecommendedPageDao {
    @Query("SELECT * FROM RecommendedPage WHERE status = 0 ORDER BY timestamp DESC")
    suspend fun getNewRecommendedPages(): List<RecommendedPage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recommendedPages: List<RecommendedPage>)

    @Update
    suspend fun updateAll(recommendedPages: List<RecommendedPage>)

    @Query("DELETE FROM RecommendedPage")
    suspend fun deleteAll()
}
