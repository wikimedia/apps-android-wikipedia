package org.wikipedia.talk.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
class TalkTemplate constructor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: Int, // Warn = 0
    var order: Int,
    var title: String,
    var subject: String,
    var message: String) : Parcelable {

    override fun toString(): String {
        return title
    }
}
