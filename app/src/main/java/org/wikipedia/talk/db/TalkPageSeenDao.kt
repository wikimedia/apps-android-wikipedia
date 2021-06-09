package org.wikipedia.talk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Single

@Dao
interface TalkPageSeenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTalkPageSeen(talkPageSeen: TalkPageSeen)

    @Query("SELECT * FROM talkpageseen WHERE sha = :sha")
    fun getTalkPageSeen(sha: String): List<TalkPageSeen>

    @Query("SELECT * FROM talkpageseen")
    fun getAll(): List<TalkPageSeen>

    @Query("DELETE FROM talkpageseen")
    fun deleteAll(): Single<Unit>
}
