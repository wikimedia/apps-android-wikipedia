package org.wikipedia.talk.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class TalkTemplate constructor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: Int, // Warn = 0
    var order: Int,
    var title: String,
    var subject: String,
    var message: String) {

    override fun toString(): String {
        return title
    }
}
