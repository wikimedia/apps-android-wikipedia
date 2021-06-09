package org.wikipedia.talk.db

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "talkpageseen")
class TalkPageSeen constructor(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) var id: Int = 0,
    val sha: String
)
