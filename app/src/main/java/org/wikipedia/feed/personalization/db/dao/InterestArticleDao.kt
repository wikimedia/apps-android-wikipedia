package org.wikipedia.feed.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wikipedia.feed.personalization.db.entity.InterestArticle

@Dao
interface InterestArticleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(interestArticle: InterestArticle)

    @Delete
    suspend fun delete(interestArticle: InterestArticle)

    @Query("DELETE FROM InterestArticle")
    suspend fun deleteAll()

    @Query("SELECT * FROM InterestArticle WHERE lang = :lang")
    suspend fun getAll(lang: String): List<InterestArticle>

    @Query("SELECT * FROM InterestArticle WHERE lang = :lang ORDER BY RANDOM()")
    suspend fun getAllRandom(lang: String): List<InterestArticle>

    @Query("SELECT EXISTS(SELECT 1 FROM InterestArticle)")
    fun hasAnyArticles(): Flow<Boolean>
}
