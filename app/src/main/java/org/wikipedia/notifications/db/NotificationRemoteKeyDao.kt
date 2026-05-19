package org.wikipedia.notifications.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationRemoteKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(remoteKey: NotificationRemoteKey)

    @Query("SELECT * FROM NotificationRemoteKey WHERE wiki = :wiki")
    suspend fun getRemoteKey(wiki: String): NotificationRemoteKey?

    @Query("DELETE FROM NotificationRemoteKey WHERE wiki = :wiki")
    suspend fun delete(wiki: String)

    @Query("DELETE FROM NotificationRemoteKey")
    suspend fun deleteAll()
}
