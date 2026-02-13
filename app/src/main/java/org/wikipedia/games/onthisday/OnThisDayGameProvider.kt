package org.wikipedia.games.onthisday

import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

object OnThisDayGameProvider {

    suspend fun getGameEvents(wikiSite: WikiSite, date: LocalDate): List<OnThisDay.Event> {
        val currentMonth = date.monthValue
        val currentDay = date.dayOfMonth

        val eventsFromApi = ServiceFactory.getRest(wikiSite).getOnThisDay(currentMonth, currentDay).events

        // Here is the logic for arranging the events:
        // First we filter out any events that actually mention a year in the text, since those might give away the answer.
        val yearRegex = Regex(".*\\b\\d{1,4}\\b.*")
        val allEvents = eventsFromApi.filter {
            it.year > 0 && it.year <= date.year && !it.text.matches(yearRegex)
        }.distinctBy { it.year }.toMutableList()

        // Shuffle the events, but seed the random number generator with the current month and day so that the order is consistent for the same day.
        allEvents.shuffle(Random(currentMonth * 100 + currentDay))

        val events = mutableListOf<OnThisDay.Event>()
        // Take an event from the list, and find another event that is within a certain range
        for (i in 0 until Prefs.otdGameQuestionsPerDay) {
            val event1 = allEvents.removeAt(0)
            var event2: OnThisDay.Event? = null
            val yearSpread = max((390 - (0.19043 * event1.year)).toInt(), 5)
            event2 = allEvents.find { abs(event1.year - it.year) <= yearSpread }
            if (event2 == null) {
                var minDiff = Int.MAX_VALUE
                for (event in allEvents) {
                    val diff = abs(event1.year - event.year)
                    if (diff < minDiff) {
                        minDiff = diff
                        event2 = event
                    }
                }
            }
            event2?.let {
                events.add(event1)
                events.add(event2)
                allEvents.remove(event2)
            }
        }
        return events
    }
}
