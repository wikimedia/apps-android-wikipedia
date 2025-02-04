package org.wikipedia.games.onthisday

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import org.wikipedia.util.log.L
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class OnThisDayGameViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<GameState>>()
    val gameState: LiveData<Resource<GameState>> get() = _gameState

    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE) ?: Constants.InvokeSource.INTENT_SHARE
    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE) ?: WikipediaApp.instance.wikiSite

    private lateinit var currentState: GameState
    private val overrideDate = savedStateHandle.contains(EXTRA_DATE)
    val currentDate = if (overrideDate) LocalDate.ofInstant(Instant.ofEpochSecond(savedStateHandle.get<Long>(EXTRA_DATE)!!), ZoneOffset.UTC) else LocalDate.now()
    val currentMonth get() = currentDate.monthValue
    val currentDay get() = currentDate.dayOfMonth

    private val events = mutableListOf<OnThisDay.Event>()
    val savedPages = mutableListOf<PageSummary>()

    init {
        Prefs.lastOtdGameVisitDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        loadGameState()
    }

    fun loadGameState() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            _gameState.postValue(Resource.Error(throwable))
        }) {
            _gameState.postValue(Resource.Loading())

            val eventsFromApi = ServiceFactory.getRest(wikiSite).getOnThisDay(currentMonth, currentDay).events

            // Here is the logic for arranging the events:
            // First we filter out any events that actually mention a year in the text, since those might give away the answer.
            val yearRegex = Regex(".*\\b\\d{1,4}\\b.*")
            val allEvents = eventsFromApi.filter {
                it.year > 0 && it.year <= currentDate.year && !it.text.matches(yearRegex)
            }.distinctBy { it.year }.toMutableList()

            // Shuffle the events, but seed the random number generator with the current month and day so that the order is consistent for the same day.
            allEvents.shuffle(Random(currentMonth * 100 + currentDay))

            events.clear()
            // Take an event from the list, and find another event that is within a certain range
            for (i in 0 until Prefs.otdGameQuestionsPerDay) {
                val event1 = allEvents.removeAt(0)
                var event2: OnThisDay.Event? = null
                var yearSpread = max((390 - (0.19043 * event1.year)).toInt(), 5)
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

            val totalState = JsonUtil.decodeFromString<TotalGameState>(Prefs.otdGameState) ?: TotalGameState()
            totalState.langToState[wikiSite.languageCode]?.let {
                currentState = it
                if (overrideDate) {
                    currentState = it.copy(currentQuestionState = composeQuestionState(0))
                }
            } ?: run {
                currentState = GameState(currentQuestionState = composeQuestionState(0))
            }

            savedPages.clear()
            getArticlesMentioned().forEach { pageSummary ->
                val inAnyList = AppDatabase.instance.readingListPageDao().findPageInAnyList(pageSummary.getPageTitle(wikiSite)) != null
                if (inAnyList) {
                    savedPages.add(pageSummary)
                }
            }

            if (currentState.currentQuestionState.month == currentMonth && currentState.currentQuestionState.day == currentDay &&
                currentState.currentQuestionIndex == 0 && !currentState.currentQuestionState.goToNext) {
                // we're just starting today's game.
                currentState = currentState.copy()
                _gameState.postValue(GameStarted(currentState))
            } else if (currentState.currentQuestionState.month == currentMonth && currentState.currentQuestionState.day == currentDay &&
                currentState.currentQuestionIndex >= currentState.totalQuestions) {
                // we're already done for today.
                val totalHistory = Prefs.otdGameHistory.let { JsonUtil.decodeFromString<TotalGameHistory>(it)?.langToHistory }?.toMutableMap() ?: mutableMapOf()
                val history = totalHistory[wikiSite.languageCode] ?: GameHistory()
                _gameState.postValue(GameEnded(currentState, history))
            } else if (currentState.currentQuestionState.month != currentMonth || currentState.currentQuestionState.day != currentDay &&
                currentState.currentQuestionIndex >= currentState.totalQuestions) {
                // we're coming back from a previous day's completed game, so start a new day's game.
                currentState = currentState.copy(currentQuestionState = composeQuestionState(0), currentQuestionIndex = 0, answerState = List(MAX_QUESTIONS) { false })
                _gameState.postValue(GameStarted(currentState))
            } else {
                // we're in the middle of a game.
                if (currentState.currentQuestionState.goToNext) {
                    // the user must have exited the activity before going to the next question,
                    // so we can fake submitting the current question.
                    submitCurrentResponse(0)
                } else {
                    // we're truly in the middle of a game, and in the middle of the current question.
                    _gameState.postValue(CurrentQuestion(currentState))
                }
            }

            persistState()
        }
    }

    fun submitCurrentResponse(selectedYear: Int) {
        if (currentState.currentQuestionState.goToNext) {
            val nextQuestionIndex = currentState.currentQuestionIndex + 1

            if (nextQuestionIndex >= currentState.totalQuestions) {
                currentState = currentState.copy(currentQuestionIndex = nextQuestionIndex)

                // push today's answers to the history map
                val totalHistory = Prefs.otdGameHistory.let { JsonUtil.decodeFromString<TotalGameHistory>(it) } ?: TotalGameHistory()
                val langToHistory = totalHistory.langToHistory.toMutableMap()
                val history = langToHistory[wikiSite.languageCode] ?: GameHistory()
                val map = history.history.toMutableMap()
                if (!map.containsKey(currentDate.year))
                    map[currentDate.year] = emptyMap()
                val monthMap = map[currentDate.year]!!.toMutableMap()
                map[currentDate.year] = monthMap
                if (!monthMap.containsKey(currentMonth))
                    monthMap[currentMonth] = emptyMap()
                val dayMap = monthMap[currentMonth]!!.toMutableMap()
                monthMap[currentMonth] = dayMap
                dayMap[currentDay] = currentState.answerState

                val newHistory = GameHistory(map)
                langToHistory[wikiSite.languageCode] = newHistory
                Prefs.otdGameHistory = JsonUtil.encodeToString(TotalGameHistory(langToHistory)).orEmpty()

                _gameState.postValue(GameEnded(currentState, newHistory))
            } else {
                currentState = currentState.copy(currentQuestionState = composeQuestionState(nextQuestionIndex), currentQuestionIndex = nextQuestionIndex)
                _gameState.postValue(CurrentQuestion(currentState))
            }
        } else {
            currentState = currentState.copy(currentQuestionState = currentState.currentQuestionState.copy(yearSelected = selectedYear, goToNext = true))

            val isCorrect = selectedYear == min(currentState.currentQuestionState.event1.year, currentState.currentQuestionState.event2.year)

            currentState = currentState.copy(
                answerState = currentState.answerState.toMutableList().apply { set(currentState.currentQuestionIndex, isCorrect) }
            )

            if (isCorrect) {
                _gameState.postValue(CurrentQuestionCorrect(currentState))
            } else {
                _gameState.postValue(CurrentQuestionIncorrect(currentState))
            }
        }
        persistState()
    }

    fun resetCurrentDayState() {
        currentState = currentState.copy(currentQuestionState = composeQuestionState(0), currentQuestionIndex = 0, answerState = List(MAX_QUESTIONS) { false })
        persistState()
    }

    fun getCurrentGameState(): GameState {
        return currentState
    }

    fun getArticlesMentioned(): List<PageSummary> {
        val articles = mutableListOf<PageSummary>()
        events.forEach { event ->
            articles.addAll(event.pages)
        }
        return articles.distinctBy { it.apiTitle }
    }

    private fun composeQuestionState(index: Int): QuestionState {
        return QuestionState(events[index * 2], events[index * 2 + 1], currentMonth, currentDay)
    }

    private fun persistState() {
        val totalState = JsonUtil.decodeFromString<TotalGameState>(Prefs.otdGameState) ?: TotalGameState()
        val langToState = totalState.langToState.toMutableMap()
        langToState[wikiSite.languageCode] = currentState
        Prefs.otdGameState = JsonUtil.encodeToString(TotalGameState(langToState)).orEmpty()
    }

    @Serializable
    data class TotalGameState(
        val langToState: Map<String, GameState> = emptyMap(),
    )

    @Serializable
    data class GameState(
        val totalQuestions: Int = Prefs.otdGameQuestionsPerDay,
        val currentQuestionIndex: Int = 0,

        // history of today's answers (correct vs incorrect)
        val answerState: List<Boolean> = List(MAX_QUESTIONS) { false },

        val currentQuestionState: QuestionState
    )

    @Serializable
    data class TotalGameHistory(
        val langToHistory: Map<String, GameHistory> = emptyMap()
    )

    @Serializable
    data class GameHistory(
        // map of:   year: month: day: list of answers
        val history: Map<Int, Map<Int, Map<Int, List<Boolean>>>> = emptyMap()
    )

    @Serializable
    data class QuestionState(
        val event1: OnThisDay.Event,
        val event2: OnThisDay.Event,
        val month: Int = 0,
        val day: Int = 0,
        val yearSelected: Int? = null,
        val goToNext: Boolean = false
    )

    class CurrentQuestion(val data: GameState) : Resource<GameState>()
    class CurrentQuestionCorrect(val data: GameState) : Resource<GameState>()
    class CurrentQuestionIncorrect(val data: GameState) : Resource<GameState>()
    class GameStarted(val data: GameState) : Resource<GameState>()
    class GameEnded(val data: GameState, val history: GameHistory) : Resource<GameState>()

    companion object {
        const val MAX_QUESTIONS = 5
        const val EXTRA_DATE = "date"
    }
}
