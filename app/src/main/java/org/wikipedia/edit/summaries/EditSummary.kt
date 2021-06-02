package org.wikipedia.edit.summaries

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "editsummaries")
class EditSummary constructor(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) var id: Int = 0,
    val summary: String,
    val lastUsed: Date = Date()) {

    override fun toString(): String {
        return summary
    }
}
