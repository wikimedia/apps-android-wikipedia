package org.wikipedia.feed.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wikipedia.feed.personalization.db.ArticleWithTopic
import org.wikipedia.feed.personalization.db.entity.InterestArticle
import org.wikipedia.page.Namespace

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

    @Query("UPDATE InterestArticle SET topicId = :newTopicId WHERE apiTitle = :apiTitle AND lang = :lang AND namespace = :namespace")
    suspend fun updateTopic(newTopicId: String, apiTitle: String, lang: String, namespace: Namespace)

    @Query("""
        SELECT 
            InterestArticle.*,
            InterestTopic.topicId AS topic_topicId,
            InterestTopic.lang AS topic_lang,
            InterestTopic.topicLabel AS topic_topicLabel,
            InterestTopic.queryTopicId AS topic_queryTopicId
        FROM InterestArticle
        INNER JOIN InterestTopic
            ON InterestArticle.topicId = InterestTopic.topicId AND InterestArticle.topicLang = InterestTopic.lang
        WHERE InterestArticle.lang = :lang
    """
    )
    suspend fun getArticlesWithTopic(lang: String): List<ArticleWithTopic>
}
