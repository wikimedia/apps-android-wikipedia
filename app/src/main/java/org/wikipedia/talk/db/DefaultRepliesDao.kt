package org.wikipedia.talk.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.rxjava3.core.Completable

@Dao
interface DefaultRepliesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insetDefaultReply(defaultReply: DefaultReplies)

    @Query("SELECT * FROM DefaultReplies WHERE text LIKE :text ORDER BY itemOrder DESC")
    fun getDefaultReplies(text: String): List<DefaultReplies>

    @Query("SELECT * FROM DefaultReplies")
    fun getAll(): List<DefaultReplies>

    @Query("DELETE FROM DefaultReplies")
    fun deleteAll(): Completable
}
