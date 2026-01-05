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
    var gameData: String?, // while game is in progress save all events data, when game is complete save just the results
    var status: Int = 1, // 0 for game in progress, 1 for game completed
)
