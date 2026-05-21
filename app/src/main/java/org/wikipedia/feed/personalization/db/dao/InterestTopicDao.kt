package org.wikipedia.feed.personalization.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wikipedia.feed.personalization.db.entity.InterestTopic

@Dao
interface InterestTopicDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(interestTopic: InterestTopic)

    @Delete
    suspend fun delete(interestTopic: InterestTopic)

    @Query("DELETE FROM InterestTopic")
    suspend fun deleteAll()

    @Query("SELECT * FROM InterestTopic")
    suspend fun getAll(): List<InterestTopic>

    @Query("SELECT * FROM InterestTopic ORDER BY RANDOM()")
    suspend fun getAllRandom(): List<InterestTopic>

    @Query("SELECT EXISTS(SELECT 1 FROM InterestTopic)")
    fun hasAnyTopics(): Flow<Boolean>
}
