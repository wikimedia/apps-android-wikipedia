package org.wikipedia.categories.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @Query("UPDATE category SET count = count + :count WHERE title = :title AND :lang")
    suspend fun incrementCount(title: String, lang: String, count: Long)

    @Query("INSERT INTO category (title, lang, count, date) VALUES (:title, :lang, :count, :date) ON CONFLICT(title, lang) DO UPDATE SET count = count + :count")
    suspend fun insertOrIncrement(title: String, lang: String, count: Long = 1, date: Long = System.currentTimeMillis())

    @Query("DELETE FROM Category")
    suspend fun deleteAll()
}