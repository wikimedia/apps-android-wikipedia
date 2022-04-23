package org.wikipedia.offline.db

import androidx.collection.ArraySet
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OfflineObject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val lang: String,
    val path: String,
    var status: Int,
    var usedByStr: String = "") {

    val usedBy: List<Long> get() {
        return usedByStr.split('|').filter { it.isNotEmpty() }.map {
            it.toLong()
        }
    }

    fun addUsedBy(id: Long) {
        val set = ArraySet(usedBy)
        set.add(id)
        updateUsedBy(set)
    }

    fun removeUsedBy(id: Long) {
        val set = ArraySet(usedBy)
        set.remove(id)
        updateUsedBy(set)
    }

    private fun updateUsedBy(set: Set<Long>) {
        usedByStr = "|${set.joinToString("|")}|"
    }
}
