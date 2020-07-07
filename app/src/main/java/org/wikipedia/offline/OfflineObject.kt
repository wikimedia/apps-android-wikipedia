package org.wikipedia.offline

import java.util.*

data class OfflineObject(val url: String, val lang: String, val path: String, var status: Int) {
    val usedBy: List<Long> = ArrayList()
}
