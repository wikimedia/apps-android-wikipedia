package org.wikipedia.games.onthisday.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

@Dao
interface DailyGameHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyGameHistory: DailyGameHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyGameHistories: List<DailyGameHistory>)

    @Update
    suspend fun update(dailyGameHistory: DailyGameHistory)

    @Delete
    suspend fun delete(dailyGameHistory: DailyGameHistory)
}
