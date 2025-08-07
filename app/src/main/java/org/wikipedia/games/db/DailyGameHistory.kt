package org.wikipedia.games.db

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity
data class DailyGameHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameName: Int,
    val language: String,
    val year: Int,
    var month: Int,
    var day: Int,
    var score: Int,
    var playType: Int,
    var gameData: String?
) {
    @Ignore
    val date: LocalDate = LocalDate.of(year, month, day)
}
