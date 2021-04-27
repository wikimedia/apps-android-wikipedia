package org.wikipedia.readinglist.database

import android.text.TextUtils
import org.apache.commons.lang3.StringUtils
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.readinglist.database.ReadingListPage
import java.io.Serializable
import java.util.*

class ReadingList(private var title: String, private var description: String?) : Serializable {
    private val pages: List<ReadingListPage> = ArrayList()
    private var id: Long = 0
    private var mtime: Long
    private var atime: Long
    private var sizeBytes: Long = 0
    private var dirty = true
    private var remoteId: Long = 0

    @Transient
    private var accentAndCaseInvariantTitle: String? = null
    fun pages(): List<ReadingListPage> {
        return pages
    }

    fun numPagesOffline(): Int {
        var count = 0
        for (page in pages) {
            if (page.offline() && page.status() == ReadingListPage.STATUS_SAVED) {
                count++
            }
        }
        return count
    }

    val isDefault: Boolean
        get() = TextUtils.isEmpty(title)

    fun id(): Long {
        return id
    }

    fun id(id: Long) {
        this.id = id
    }

    fun title(): String {
        return if (isDefault) WikipediaApp.getInstance().getString(R.string.default_reading_list_name) else title
    }

    fun title(title: String) {
        this.title = title
    }

    fun dbTitle(): String {
        return title
    }

    fun accentAndCaseInvariantTitle(): String {
        if (accentAndCaseInvariantTitle == null) {
            accentAndCaseInvariantTitle = StringUtils.stripAccents(title).toLowerCase()
        }
        return accentAndCaseInvariantTitle!!
    }

    fun description(): String? {
        return description
    }

    fun description(description: String?) {
        this.description = description
    }

    fun mtime(): Long {
        return mtime
    }

    fun mtime(mtime: Long) {
        this.mtime = mtime
    }

    fun atime(): Long {
        return atime
    }

    fun atime(atime: Long) {
        this.atime = atime
    }

    fun touch() {
        atime = System.currentTimeMillis()
    }

    fun sizeBytes(): Long {
        var bytes: Long = 0
        for (page in pages) {
            bytes += if (page.offline()) page.sizeBytes() else 0
        }
        return bytes
    }

    fun sizeBytes(sizeBytes: Long) {
        this.sizeBytes = sizeBytes
    }

    fun dirty(): Boolean {
        return dirty
    }

    fun dirty(dirty: Boolean) {
        this.dirty = dirty
    }

    fun remoteId(): Long {
        return remoteId
    }

    fun remoteId(remoteId: Long) {
        this.remoteId = remoteId
    }

    companion object {
        const val SORT_BY_NAME_ASC = 0
        const val SORT_BY_NAME_DESC = 1
        const val SORT_BY_RECENT_ASC = 2
        const val SORT_BY_RECENT_DESC = 3
        @JvmField
        val DATABASE_TABLE = ReadingListTable()
        fun sort(list: ReadingList, sortMode: Int) {
            when (sortMode) {
                SORT_BY_NAME_ASC -> list.pages().sortedWith { lhs: ReadingListPage, rhs: ReadingListPage -> lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle()) }
                SORT_BY_NAME_DESC -> list.pages().sortedWith { lhs: ReadingListPage, rhs: ReadingListPage -> rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle()) }
                SORT_BY_RECENT_ASC -> list.pages().sortedWith { lhs: ReadingListPage, rhs: ReadingListPage -> lhs.mtime().compareTo(rhs.mtime()) }
                SORT_BY_RECENT_DESC -> list.pages().sortedWith { lhs: ReadingListPage, rhs: ReadingListPage -> rhs.mtime().compareTo(lhs.mtime()) }
                else -> {
                }
            }
        }

        fun sort(lists: MutableList<ReadingList>, sortMode: Int) {
            when (sortMode) {
                SORT_BY_NAME_ASC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle()) }
                SORT_BY_NAME_DESC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle()) }
                SORT_BY_RECENT_ASC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> rhs.mtime().compareTo(lhs.mtime()) }
                SORT_BY_RECENT_DESC -> lists.sortWith { lhs: ReadingList, rhs: ReadingList -> lhs.mtime().compareTo(rhs.mtime()) }
                else -> {
                }
            }
            // make the Default list sticky on top, regardless of sorting.
            var defaultList: ReadingList? = null
            for (list in lists) {
                if (list.isDefault) {
                    defaultList = list
                    break
                }
            }
            if (defaultList != null) {
                lists.remove(defaultList)
                lists.add(0, defaultList)
            }
        }

        fun sortGenericList(lists: MutableList<Any>, sortMode: Int) {
            when (sortMode) {
                SORT_BY_NAME_ASC -> Collections.sort(lists) { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        return@sort lhs.accentAndCaseInvariantTitle().compareTo(rhs.accentAndCaseInvariantTitle())
                    } else {
                        return@sort 0
                    }
                }
                SORT_BY_NAME_DESC -> Collections.sort(lists) { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        return@sort rhs.accentAndCaseInvariantTitle().compareTo(lhs.accentAndCaseInvariantTitle())
                    } else {
                        return@sort 0
                    }
                }
                SORT_BY_RECENT_ASC -> Collections.sort(lists) { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        return@sort java.lang.Long.compare(rhs.mtime(), lhs.mtime())
                    } else {
                        return@sort 0
                    }
                }
                SORT_BY_RECENT_DESC -> Collections.sort(lists) { lhs: Any?, rhs: Any? ->
                    if (lhs is ReadingList && rhs is ReadingList) {
                        return@sort java.lang.Long.compare(lhs.mtime(), rhs.mtime())
                    } else {
                        return@sort 0
                    }
                }
                else -> {
                }
            }
            // make the Default list sticky on top, regardless of sorting.
            var defaultList: ReadingList? = null
            for (list in lists) {
                if (list is ReadingList && list.isDefault) {
                    defaultList = list
                    break
                }
            }
            if (defaultList != null) {
                lists.remove(defaultList)
                lists.add(0, defaultList)
            }
        }
    }

    init {
        val now = System.currentTimeMillis()
        mtime = now
        atime = now
    }
}