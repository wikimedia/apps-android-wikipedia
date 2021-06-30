package org.wikipedia.feed.onthisday

import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.annotations.Required

class OnThisDay {

    private val events: List<Event> = emptyList()
    private val births: List<Event> = emptyList()
    private val deaths: List<Event> = emptyList()
    private val holidays: List<Event> = emptyList()
    var selected: List<Event> = emptyList()

    fun allEvents(): List<Event> {
        return (events + births + deaths + holidays).sortedByDescending { it.year }
    }

    class Event {

        private val pages: List<PageSummary?>? = null
        val text = ""
        val year = 0

        fun pages(): List<PageSummary>? {
            return pages?.filterNotNull()
        }
    }
}
