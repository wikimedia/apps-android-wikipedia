package org.wikipedia.talk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TalkPageSeenDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTalkPageSeen(talkPageSeen: TalkPageSeen)

    @Query("SELECT * FROM TalkPageSeen WHERE sha = :sha LIMIT 1")
    fun getTalkPageSeen(sha: String): TalkPageSeen?

    @Query("SELECT * FROM TalkPageSeen")
    fun getAll(): Flow<List<TalkPageSeen>>

    @Query("DELETE FROM TalkPageSeen WHERE sha = :sha")
    suspend fun deleteTalkPageSeen(sha: String)

    @Query("DELETE FROM TalkPageSeen")
    suspend fun deleteAll()
}
