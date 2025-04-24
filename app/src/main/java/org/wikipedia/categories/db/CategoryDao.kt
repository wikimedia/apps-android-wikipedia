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

    @Query("SELECT title, lang, COUNT(*) as count FROM category where timeStamp >= :startOfYear AND timeStamp <= :endOfYear " +
            "GROUP BY title, lang ORDER BY count DESC")
    suspend fun getCategoriesByYearRange(startOfYear: Long, endOfYear: Long): List<CategoryCount>

    @Query("DELETE FROM Category")
    suspend fun deleteAll()

    @Query("DELETE FROM Category WHERE timeStamp < :timeStamp")
    suspend fun deleteOlderThan(timeStamp: Long)
}
