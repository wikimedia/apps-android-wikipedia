package org.wikipedia.categories.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>): List<Long>

    @Query("SELECT title, lang, COUNT(*) as count FROM Category where timeStamp >= :startTimeStamp AND timeStamp <= :endTimeStamp " +
            "GROUP BY title, lang ORDER BY count DESC")
    suspend fun getCategoriesByTimeRange(startTimeStamp: Long, endTimeStamp: Long): List<CategoryCount>

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("DELETE FROM Category")
    suspend fun deleteAll()

    @Query("DELETE FROM Category WHERE timeStamp < :timeStamp")
    suspend fun deleteOlderThan(timeStamp: Long)
}
