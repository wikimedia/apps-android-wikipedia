package org.wikipedia.page

import kotlinx.serialization.Serializable
import org.wikipedia.history.HistoryEntry

@Serializable
class PageBackStackItem(var title: PageTitle?, var historyEntry: HistoryEntry?, var scrollY: Int = 0) {

    constructor(title: PageTitle?, historyEntry: HistoryEntry?) : this(title, historyEntry, 0)

    init {
        // TODO: remove this crash probe upon fixing
        require(!(title == null || historyEntry == null)) { "Nonnull parameter is in fact null." }
    }
}
