package org.wikipedia.readinglist.database

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation for the one-to-many between [ReadingList] and [ReadingListPage]
 * (joined on ReadingList.id == ReadingListPage.listId).
 */
class ReadingListWithPages(
    @Embedded val list: ReadingList,
    @Relation(parentColumn = "id", entityColumn = "listId")
    val pages: List<ReadingListPage>
) {
    fun toReadingList(): ReadingList {
        return list.apply {
            pages.clear()
            pages.addAll(this@ReadingListWithPages.pages.filterNot { it.status == ReadingListPage.STATUS_QUEUE_FOR_DELETE })
        }
    }
}
