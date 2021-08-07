package org.wikipedia.feed.onthisday

import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.page.PageSummary

@JsonClass(generateAdapter = true)
class OnThisDay(
    internal val events: List<Event> = emptyList(),
    internal val births: List<Event> = emptyList(),
    internal val deaths: List<Event> = emptyList(),
    internal val holidays: List<Event> = emptyList(),
    var selected: List<Event> = emptyList()
) {
    val allEvents: List<Event>
        get() = (events + births + deaths + holidays).sortedByDescending { it.year }

    @JsonClass(generateAdapter = true)
    class Event(val pages: List<PageSummary> = emptyList(), val text: String = "", val year: Int = 0)
}
