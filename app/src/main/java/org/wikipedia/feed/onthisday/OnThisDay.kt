package org.wikipedia.feed.onthisday

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.page.PageSummary

@Serializable
class OnThisDay {

    val events: List<Event> = emptyList()
    private val births: List<Event> = emptyList()
    private val deaths: List<Event> = emptyList()
    private val holidays: List<Event> = emptyList()

    fun allEvents(): List<Event> {
        return (events + births + deaths + holidays).sortedByDescending { it.year }
    }

    @Serializable
    class Event {
        val pages: List<PageSummary> = emptyList()
        val text = ""
        val year = 0
    }
}
