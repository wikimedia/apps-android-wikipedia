package org.wikipedia.games.onthisday.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DailyGameHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyGameHistory: DailyGameHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyGameHistories: List<DailyGameHistory>)

    @Query("SELECT * FROM DailyGameHistory ORDER BY year DESC, month DESC, day DESC, id DESC LIMIT 1")
    suspend fun findLastGameHistory(): DailyGameHistory?

    @Update
    suspend fun update(dailyGameHistory: DailyGameHistory)

    @Delete
    suspend fun delete(dailyGameHistory: DailyGameHistory)
}
