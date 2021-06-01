package org.wikipedia.search

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "recentsearches")
class RecentSearch constructor(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) var id: Int = 0,
    val text: String,
    val timestamp: Date = Date())
