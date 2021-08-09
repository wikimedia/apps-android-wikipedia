package org.wikipedia.page

import com.squareup.moshi.JsonClass
import org.wikipedia.history.HistoryEntry

@JsonClass(generateAdapter = true)
class PageBackStackItem(var title: PageTitle, var historyEntry: HistoryEntry, var scrollY: Int = 0)
