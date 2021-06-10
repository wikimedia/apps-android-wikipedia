package org.wikipedia.offline.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class OfflineObject(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
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
        usedByStr = "|${set.joinToString("|")}|"
    }
}
