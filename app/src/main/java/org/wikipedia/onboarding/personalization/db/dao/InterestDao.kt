package org.wikipedia.onboarding.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wikipedia.onboarding.personalization.db.entity.Interest

@Dao
interface InterestDao {
    @Query("SELECT * FROM Interests WHERE type = :type AND lang = :lang")
    fun getByType(type: Int, lang: String): Flow<List<Interest>>

    @Insert
    suspend fun insert(interest: Interest)

    @Query("SELECT * FROM Interests WHERE topicKey = :topicId AND lang = :lang LIMIT 1")
    suspend fun findTopic(topicId: String, lang: String): Interest?

    @Query("SELECT * FROM Interests WHERE articleApiTitle = :articleApiTitle AND lang = :lang LIMIT 1")
    suspend fun findArticle(articleApiTitle: String, lang: String): Interest?

    @Delete
    suspend fun delete(interest: Interest)

    @Query("DELETE FROM Interests WHERE type = :type AND lang = :lang")
    suspend fun deleteAllByType(type: Int, lang: String)
}
