package org.wikipedia.categories.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>): List<Long>

    // find categories by primary keys
    @Query("SELECT * FROM Category WHERE year = :year AND month = :month AND title = :title AND lang = :lang")
    suspend fun findByPrimaryKey(year: Int, month: Int, title: String, lang: String): Category?

    // Update count by primary keys
    @Query("UPDATE Category SET count = count + 1 WHERE year = :year AND month = :month AND title = :title AND lang = :lang")
    suspend fun updateCountByPrimaryKeys(year: Int, month: Int, title: String, lang: String)

    @Query("SELECT year, month, title, lang, SUM (count) AS count FROM Category WHERE year BETWEEN :startYear AND :endYear GROUP BY title, lang ORDER BY count DESC")
    suspend fun getCategoriesByTimeRange(startYear: Int, endYear: Int): List<Category>

    @Query("SELECT * FROM Category")
    suspend fun getAllCategories(): List<Category>

    @Query("DELETE FROM Category")
    suspend fun deleteAll()

    @Query("DELETE FROM Category WHERE rowid IN (SELECT rowid FROM Category WHERE year < :year)")
    suspend fun deleteOlderThan(year: Int): Int

    @Transaction
    suspend fun upsertAll(list: List<Category>) {
        list.forEach { category ->
            findByPrimaryKey(category.year, category.month, category.title, category.lang)?.let {
                updateCountByPrimaryKeys(category.year, category.month, category.title, category.lang)
            } ?: insert(category)
        }
    }
}
