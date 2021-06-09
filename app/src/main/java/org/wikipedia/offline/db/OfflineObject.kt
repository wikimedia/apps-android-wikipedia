package org.wikipedia.offline.db

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offlineobject")
data class OfflineObject(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) var id: Int = 0,
    val url: String,
    val lang: String,
    val path: String,
    var status: Int,
    var usedby: String = "") {

    val usedBy: List<Long> get() {
        return usedby.split('|').filter { it.isNotEmpty() }.map {
            it.toLong()
        }
    }

    fun addUsedBy(id: Long) {
        val set = usedBy.toMutableSet()
        set.add(id)
        updateUsedBy(set)
    }

    fun removeUsedBy(id: Long) {
        val set = usedBy.toMutableSet()
        set.remove(id)
        updateUsedBy(set)
    }

    private fun updateUsedBy(set: Set<Long>) {
        usedby = "|${set.joinToString("|")}|"
    }
}
