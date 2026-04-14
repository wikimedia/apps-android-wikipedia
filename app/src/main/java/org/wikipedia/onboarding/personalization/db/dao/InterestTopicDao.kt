package org.wikipedia.onboarding.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wikipedia.onboarding.personalization.db.entity.InterestTopic

@Dao
interface InterestTopicDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(interestTopic: InterestTopic)

    @Delete
    suspend fun delete(interestTopic: InterestTopic)

    @Query("DELETE FROM InterestTopic")
    suspend fun deleteAll()

    @Query("SELECT * FROM InterestTopic WHERE lang = :lang")
    suspend fun getAll(lang: String): List<InterestTopic>
}
