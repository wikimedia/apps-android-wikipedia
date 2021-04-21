package org.wikipedia.events

import org.wikipedia.readinglist.database.ReadingListPage

class ArticleSavedOrDeletedEvent(val isAdded: Boolean, vararg pages: ReadingListPage) {
    val pages: Array<ReadingListPage> = arrayOf(*pages)
}