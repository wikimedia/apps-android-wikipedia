package org.wikipedia.games.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.wikipedia.games.PlayTypes
import java.time.LocalDate
import java.time.Month

@Dao
interface DailyGameHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dailyGameHistory: DailyGameHistory)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dailyGameHistories: List<DailyGameHistory>)

    @Query("SELECT * FROM DailyGameHistory ORDER BY year DESC, month DESC, day DESC, id DESC LIMIT 1")
    suspend fun findLastGameHistory(): DailyGameHistory?

    @Query("SELECT * FROM DailyGameHistory WHERE gameName = :gameName AND language = :language " +
            "AND year = :year AND month = :month AND day = :day")
    suspend fun findGameHistoryByDate(gameName: Int, language: String, year: Int, month: Int, day: Int): DailyGameHistory?

    @Query("SELECT COUNT(*) FROM DailyGameHistory WHERE gameName = :gameName AND language = :language AND status = 1")
    suspend fun getTotalGamesPlayed(gameName: Int, language: String): Int

    @Query("SELECT AVG(score) FROM DailyGameHistory WHERE gameName = :gameName AND language = :language AND status = 1")
    suspend fun getAverageScore(gameName: Int, language: String): Double?

    @Query("SELECT * FROM DailyGameHistory WHERE gameName = :gameName AND language = :language AND status = 1 ORDER BY year DESC, month DESC, day DESC")
    suspend fun getGameHistory(gameName: Int, language: String): List<DailyGameHistory>

    @Update
    suspend fun update(dailyGameHistory: DailyGameHistory)

    @Delete
    suspend fun delete(dailyGameHistory: DailyGameHistory)

    suspend fun getCurrentStreak(gameName: Int, language: String): Int {
        val history = getGameHistory(gameName, language).filter { it.playType == PlayTypes.PLAYED_ON_SAME_DAY.ordinal }
        if (history.isEmpty()) {
            return 0
        }

        var currentStreak = 0
        var expectedDate = LocalDate.now() // Start with today's date

        for (record in history) {
            val recordDate = LocalDate.of(record.year, Month.of(record.month), record.day)

            if (recordDate == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1) // Move to the previous day
            } else if (recordDate.isBefore(expectedDate)) {
                break
            }
        }

        return currentStreak
    }

    suspend fun getBestStreak(gameName: Int, language: String): Int {
        val history = getGameHistory(gameName, language).filter { it.playType == PlayTypes.PLAYED_ON_SAME_DAY.ordinal }
        if (history.isEmpty()) {
            return 0
        }

        var bestStreak = 0
        var currentStreak = 0
        var expectedDate = LocalDate.now() // Start with today's date

        for (record in history) {
            val recordDate = LocalDate.of(record.year, Month.of(record.month), record.day)

            if (recordDate == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1) // Move to the previous day
            } else if (recordDate.isBefore(expectedDate)) {
                bestStreak = maxOf(bestStreak, currentStreak)
                currentStreak = 0
                expectedDate = recordDate.minusDays(1) // Reset to the day before the record date
            }
        }

        return maxOf(bestStreak, currentStreak)
    }

    @Transaction
    suspend fun insertOrUpdate(dailyGameHistory: DailyGameHistory) {
        val existing = findGameHistoryByDate(dailyGameHistory.gameName, dailyGameHistory.language, dailyGameHistory.year, dailyGameHistory.month, dailyGameHistory.day)
        if (existing != null) {
            update(dailyGameHistory.copy(id = existing.id))
        } else {
            insert(dailyGameHistory)
        }
    }
}
