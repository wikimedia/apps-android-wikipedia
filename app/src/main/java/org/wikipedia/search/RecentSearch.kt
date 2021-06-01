package org.wikipedia.search

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "recentsearches")
class RecentSearch @JvmOverloads constructor(val text: String?, val timestamp: Date = Date()) {
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) var id: Int = 0


    override fun equals(other: Any?): Boolean {
        if (other !is RecentSearch) {
            return false
        }
        return text == other.text
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }
}
