package org.wikipedia.onboarding.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wikipedia.onboarding.personalization.db.entity.TopicInterest

@Dao
interface TopicInterestDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(topicInterest: TopicInterest)

    @Delete
    suspend fun delete(topicInterest: TopicInterest)

    @Query("DELETE FROM TopicInterest")
    suspend fun deleteAll()
}
