package org.wikipedia.onboarding.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wikipedia.onboarding.personalization.db.entity.ArticleInterest

@Dao
interface ArticleInterestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(articleInterest: ArticleInterest)

    @Delete
    suspend fun delete(articleInterest: ArticleInterest)

    @Query("DELETE FROM ArticleInterest")
    suspend fun deleteAll()
}
