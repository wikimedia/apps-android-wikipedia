package org.wikipedia.talk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable

@Dao
interface TalkPageSeenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTalkPageSeen(talkPageSeen: TalkPageSeen)

    @Query("SELECT * FROM TalkPageSeen WHERE sha = :sha")
    fun getTalkPageSeen(sha: String): List<TalkPageSeen>

    @Query("SELECT * FROM TalkPageSeen")
    fun getAll(): List<TalkPageSeen>

    @Query("DELETE FROM TalkPageSeen")
    fun deleteAll(): Completable
}
