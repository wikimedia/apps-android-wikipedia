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

    @Query("SELECT year, month, title, lang, SUM (count) AS count FROM Category WHERE year BETWEEN :startYear AND :endYear GROUP BY title, lang ORDER BY count DESC")
    suspend fun getCategoriesByTimeRange(startYear: Int, endYear: Int): List<Category>

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("DELETE FROM Category")
    suspend fun deleteAll()

    @Query("DELETE FROM Category WHERE rowid IN (SELECT rowid FROM Category WHERE year <:year LIMIT :batchSize)")
    suspend fun deleteOlderThanInBatch(year: Int, batchSize: Int): Int
}
