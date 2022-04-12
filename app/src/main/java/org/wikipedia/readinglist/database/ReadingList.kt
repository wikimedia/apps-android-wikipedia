package org.wikipedia.readinglist.database

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import java.io.Serializable
import java.util.*

// TODO: create default reading list upon initial DB creation.

@Entity
class ReadingList(
    var listTitle: String,
    var description: String?,
    var mtime: Long = System.currentTimeMillis(),
    var atime: Long = mtime,
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var sizeBytes: Long = 0,
    var dirty: Boolean = true,
    var remoteId: Long = 0
) : Serializable {

    @Ignore
    val pages = mutableListOf<ReadingListPage>()

    @Transient
    private var accentAndCaseInvariantTitle: String? = null

    var title
        get() = listTitle.ifEmpty { WikipediaApp.getInstance().getString(R.string.default_reading_list_name) }
        set(value) { listTitle = value }

    val isDefault
        get() = title == WikipediaApp.getInstance().getString(R.string.default_reading_list_name)

    val numPagesOffline
        get() = pages.count { it.offline && it.status == ReadingListPage.STATUS_SAVED }

    val sizeBytesFromPages
        get() = pages.sumOf { if (it.offline) it.sizeBytes else 0 }

    fun accentAndCaseInvariantTitle(): String {
        if (accentAndCaseInvariantTitle == null) {
            accentAndCaseInvariantTitle = StringUtils.stripAccents(title).lowercase(Locale.getDefault())
        }
        return accentAndCaseInvariantTitle!!
    }

    fun touch() {
        atime = System.currentTimeMillis()
    }

    fun compareTo(other: Any): Boolean {
        return (other is ReadingList &&
                id == other.id &&
                pages.size == other.pages.size &&
                numPagesOffline == numPagesOffline &&
                title == other.title &&
                description == other.description)
    }

    companion object {
        const val SORT_BY_NAME_ASC = 0
        const val SORT_BY_NAME_DESC = 1
        const val SORT_BY_RECENT_ASC = 2
        const val SORT_BY_RECENT_DESC = 3

        fun sort(list: ReadingList, sortMode: Int) {
            when (sortMode) {
                SORT_BY_NAME_ASC -> list.pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle()) }
                SORT_BY_NAME_DESC -> list.pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle()) }
                SORT_BY_RECENT_ASC -> list.pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> lhs.mtime.compareTo(rhs.mtime) }
                SORT_BY_RECENT_DESC -> list.pages.sortWith { lhs: ReadingListPage, rhs: ReadingListPage -> rhs.mtime.compareTo(lhs.mtime) }
            }
        }

        fun sort(lists: MutableList<ReadingList>, sortMode: Int) {
            when (sortMode) {
                SORT_BY_NAME_ASC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle()) }
                SORT_BY_NAME_DESC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle()) }
                SORT_BY_RECENT_ASC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> rhs.mtime.compareTo(lhs.mtime) }
                SORT_BY_RECENT_DESC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> lhs.mtime.compareTo(rhs.mtime) }
            }
            // make the Default list sticky on top, regardless of sorting.
            lists.firstOrNull { it.isDefault }?.let {
                lists.remove(it)
                lists.add(0, it)
            }
        }

        fun sortGenericList(lists: MutableList<Any>, sortMode: Int) {
            when (sortMode) {
                SORT_BY_NAME_ASC -> lists.sortWith { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle())
                    } else {
                        0
                    }
                }
                SORT_BY_NAME_DESC -> lists.sortWith { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle())
                    } else {
                        0
                    }
                }
                SORT_BY_RECENT_ASC -> lists.sortWith { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        rhs.mtime.compareTo(lhs.mtime)
                    } else {
                        0
                    }
                }
                SORT_BY_RECENT_DESC -> lists.sortWith { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        lhs.mtime.compareTo(rhs.mtime)
                    } else {
                        0
                    }
                }
            }

            // make the Default list sticky on top, regardless of sorting.
            lists.firstOrNull { it is ReadingList && it.isDefault }?.let {
                lists.remove(it)
                lists.add(0, it)
            }
        }
    }
}
