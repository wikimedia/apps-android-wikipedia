package org.wikipedia.search

import java.util.*

class RecentSearch @JvmOverloads constructor(val text: String?, val timestamp: Date = Date()) {

    override fun equals(other: Any?): Boolean {
        if (other !is RecentSearch) {
            return false
        }
        return text == other.text
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }

    override fun toString(): String {
        return "RecentSearch{" +
                "text=" + text +
                ", timestamp=" + timestamp.time +
                "}"
    }

    companion object {
        @JvmField
        val DATABASE_TABLE = RecentSearchDatabaseTable()
    }
}
