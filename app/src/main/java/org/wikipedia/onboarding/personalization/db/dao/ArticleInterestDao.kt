package org.wikipedia.onboarding.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wikipedia.onboarding.personalization.db.entity.ArticleInterest
import org.wikipedia.page.Namespace

@Dao
interface ArticleInterestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(articleInterest: ArticleInterest)

    @Delete
    suspend fun delete(articleInterest: ArticleInterest)

    @Query("DELETE FROM ArticleInterest")
    suspend fun deleteAll()

    @Query("SELECT * FROM ArticleInterest WHERE lang = :lang")
    suspend fun getAll(lang: String): List<ArticleInterest>

    @Query("UPDATE ArticleInterest SET topicId = :newTopicId WHERE apiTitle = :apiTitle AND lang = :lang AND namespace = :namespace")
    suspend fun updateTopic(newTopicId: String, apiTitle: String, lang: String, namespace: Namespace)
}
