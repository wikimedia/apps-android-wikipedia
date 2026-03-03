package org.wikipedia.games.onthisday

import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.feed.wikigames.OnThisDayCardGameState
import org.wikipedia.games.WikiGames
import org.wikipedia.games.db.DailyGameHistory
import org.wikipedia.settings.Prefs
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

object OnThisDayGameProvider {

    suspend fun getGameEvents(wikiSite: WikiSite, date: LocalDate): List<OnThisDay.Event> {
        val currentMonth = date.monthValue
        val currentDay = date.dayOfMonth

        val events = mutableListOf<OnThisDay.Event>()
        val eventsFromApi = ServiceFactory.getRest(wikiSite).getOnThisDay(currentMonth, currentDay).events

        // Here is the logic for arranging the events:
        // First we filter out any events that actually mention a year in the text, since those might give away the answer.
        val yearRegex = Regex(".*\\b\\d{1,4}\\b.*")
        val allEvents = eventsFromApi.filter {
            it.year > 0 && it.year <= date.year && !it.text.matches(yearRegex)
        }.distinctBy { it.year }.toMutableList()

        // Shuffle the events, but seed the random number generator with the current month and day so that the order is consistent for the same day.
        allEvents.shuffle(Random(currentMonth * 100 + currentDay))
        // Make a copy of the list of events, to draw from in case the allEvents list doesn't have enough events.
        val allEventsCopy = allEvents.toList()

        // Take an event from the list, and find another event that is within a certain range
        repeat(Prefs.otdGameQuestionsPerDay) { index ->
            val event1 = if (allEvents.isNotEmpty()) allEvents.removeAt(0) else allEventsCopy[index % allEventsCopy.size]
            var event2: OnThisDay.Event?
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
            if (event2 == null) {
                event2 = allEventsCopy.find { it.year != event1.year }
            }
            event2?.let {
                events.add(event1)
                events.add(event2)
                allEvents.remove(event2)
            }
        }
        return events
    }

    suspend fun getGameState(wikiSite: WikiSite, date: LocalDate): OnThisDayCardGameState {
        val currentMonth = date.monthValue
        val currentDay = date.dayOfMonth

        val gameHistory = AppDatabase.instance.dailyGameHistoryDao().findGameHistoryByDate(
            gameName = WikiGames.WHICH_CAME_FIRST.ordinal,
            language = wikiSite.languageCode,
            year = date.year,
            month = currentMonth,
            day = currentDay
        )

        if (gameHistory != null) {
            if (gameHistory.status == DailyGameHistory.GAME_COMPLETED) {
                return OnThisDayCardGameState.Completed(langCode = wikiSite.languageCode, score = gameHistory.score, totalQuestions = Prefs.otdGameQuestionsPerDay)
            } else if (gameHistory.status == DailyGameHistory.GAME_IN_PROGRESS) {
                return OnThisDayCardGameState.InProgress(langCode = wikiSite.languageCode, currentQuestion = gameHistory.currentQuestionIndex)
            }
        }

        val events = getGameEvents(wikiSite, date)
        return OnThisDayCardGameState.Preview(langCode = wikiSite.languageCode, event1 = events[0], event2 = events[1])
    }

    fun getThumbnailUrlForEvent(event: OnThisDay.Event): String? {
        return event.pages.firstOrNull { !it.thumbnailUrl.isNullOrEmpty() }?.thumbnailUrl
    }
}
