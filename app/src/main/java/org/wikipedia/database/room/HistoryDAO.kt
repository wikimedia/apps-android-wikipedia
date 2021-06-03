package org.wikipedia.database.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HistoryDAO {
    @Query("SELECT * FROM his")
    fun getAllHistoryItems(): Array<History>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUsers(vararg users: History)
}
