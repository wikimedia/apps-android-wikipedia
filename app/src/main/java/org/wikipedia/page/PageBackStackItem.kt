package org.wikipedia.page

import kotlinx.serialization.Serializable
import org.wikipedia.history.HistoryEntry

@Serializable
class PageBackStackItem(var title: PageTitle, var historyEntry: HistoryEntry, var scrollY: Int = 0)
