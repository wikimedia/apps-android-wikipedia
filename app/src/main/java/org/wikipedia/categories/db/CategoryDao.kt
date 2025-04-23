package org.wikipedia.categories.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("SELECT * FROM category WHERE title = :title AND lang = :lang")
    suspend fun getCategoryByTitleAndLang(title: String, lang: String): Category?

    @Query("UPDATE category SET count = count + :count WHERE title = :title AND :lang")
    suspend fun incrementCount(title: String, lang: String, count: Long)

    // raw query sqlite supports this but ROOM compiler shows a warning here
    @Query("INSERT INTO category (title, lang, count, year) VALUES (:title, :lang, :count, :year) ON CONFLICT(title, lang) DO UPDATE SET count = count + :count")
    suspend fun insertOrIncrement(title: String, lang: String, count: Long = 1, year: Long)

    // ROOM style
    @Transaction
    suspend fun upsert(category: Category) {
        val existingCategory = getCategoryByTitleAndLang(category.title, category.lang)
        if (existingCategory != null) {
            // If category exists,
            // update count by adding the new count value and
            // update date to current date
            val newCount = existingCategory.count + category.count
            val newYear = existingCategory.year
            update(Category(existingCategory.title, existingCategory.lang, newCount, newYear))
        } else {
            // Otherwise insert new category
            insert(category)
        }
    }

    @Query("SELECT * FROM category where year = :year order by count DESC")
    suspend fun getCategoriesByYear(year: Long): List<Category>

    @Query("DELETE FROM Category")
    suspend fun deleteAll()
}
