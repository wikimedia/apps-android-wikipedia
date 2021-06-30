package org.wikipedia.feed.onthisday

import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.json.annotations.Required
import java.util.*

class OnThisDay {

    private val events: List<Event>? = null
    private val births: List<Event>? = null
    private val deaths: List<Event>? = null
    private val holidays: List<Event>? = null
    var selected: List<Event> = emptyList()

    fun events(): List<Event> {
        val allEvents = ArrayList<Event>()
        events?.let { allEvents.addAll(it) }
        births?.let { allEvents.addAll(it) }
        deaths?.let { allEvents.addAll(it) }
        holidays?.let { allEvents.addAll(it) }

        allEvents.sortWith { e1: Event, e2: Event ->
            e2.year.compareTo(e1.year)
        }
        return allEvents
    }

    class Event {

        @Required
        val text: String = ""

        @Required
        private val pages: MutableList<PageSummary>? = null
        val year = 0

        fun pages(): List<PageSummary>? {
            pages?.let {
                val iterator: MutableIterator<*> = pages.iterator()
                while (iterator.hasNext()) {
                    if (iterator.next() == null) {
                        iterator.remove()
                    }
                }
            }
            return pages
        }
    }
}
