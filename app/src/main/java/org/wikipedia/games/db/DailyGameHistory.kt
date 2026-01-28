package org.wikipedia.games.db

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    var gameData: String?,
    var status: Int = GAME_IN_PROGRESS,
    var currentQuestionIndex: Int = 0
) {
    companion object {
        const val GAME_IN_PROGRESS = 0
        const val GAME_COMPLETED = 1
    }
}
