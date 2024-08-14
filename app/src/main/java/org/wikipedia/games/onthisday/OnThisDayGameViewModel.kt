package org.wikipedia.games.onthisday

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.math.abs
import kotlin.random.Random

class OnThisDayGameViewModel(bundle: Bundle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<GameState>>()
    val gameState: LiveData<Resource<GameState>> get() = _gameState

    val invokeSource = bundle.getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource

    private lateinit var currentState: GameState
    private val currentDate = LocalDate.now()
    val currentMonth = currentDate.monthValue
    val currentDay = currentDate.dayOfMonth

    private val events = mutableListOf<OnThisDay.Event>()

    init {
        loadGameState()
    }

    fun loadGameState() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _gameState.postValue(Resource.Error(throwable))
        }) {
            _gameState.postValue(Resource.Loading())

            events.clear()
            events.addAll(ServiceFactory.getRest(WikipediaApp.instance.wikiSite).getOnThisDay(currentMonth, currentDay).events)

            JsonUtil.decodeFromString<GameState>(Prefs.otdGameState)?.let {
                currentState = it
            } ?: run {
                currentState = GameState(currentQuestionState = composeQuestionState(currentMonth, currentDay, 0))
            }

            if (currentState.currentQuestionState.month == currentMonth && currentState.currentQuestionState.day == currentDay &&
                currentState.currentQuestionIndex == 0 && !currentState.currentQuestionState.goToNext) {
                // we're just starting today's game.
                _gameState.postValue(GameStarted(currentState))
            } else if (currentState.currentQuestionState.month == currentMonth && currentState.currentQuestionState.day == currentDay &&
                currentState.currentQuestionIndex >= currentState.totalQuestions) {
                // we're already done for today.
                _gameState.postValue(GameEnded(currentState))
            } else {
                _gameState.postValue(Resource.Success(currentState))
            }

            persistState()
        }
    }

    fun submitCurrentResponse(selectedYear: Int) {
        currentState = currentState.copy(currentQuestionState = currentState.currentQuestionState.copy(yearSelected = selectedYear))

        if (currentState.currentQuestionState.goToNext) {
            val nextQuestionIndex = currentState.currentQuestionIndex + 1
            currentState = currentState.copy(currentQuestionState = composeQuestionState(currentMonth, currentDay, nextQuestionIndex), currentQuestionIndex = nextQuestionIndex)

            if (nextQuestionIndex >= currentState.totalQuestions) {

                // push today's answers to the history map
                currentState = currentState.copy(answerStateHistory = currentState.answerStateHistory + mapOf(currentDate.year to mapOf(currentMonth to mapOf(currentDay to currentState.answerState))))

                _gameState.postValue(GameEnded(currentState))
            } else {
                _gameState.postValue(Resource.Success(currentState))
            }
        } else {
            currentState = currentState.copy(currentQuestionState = currentState.currentQuestionState.copy(goToNext = true))

            val isCorrect = currentState.currentQuestionState.event.year == selectedYear
            currentState = currentState.copy(answerState = currentState.answerState.toMutableList().apply { set(currentState.currentQuestionIndex, isCorrect) })

            if (isCorrect) {
                _gameState.postValue(CurrentQuestionCorrect(currentState))
            } else {
                _gameState.postValue(CurrentQuestionIncorrect(currentState))
            }
        }
        persistState()
    }

    fun resetCurrentDay() {
        currentState = currentState.copy(currentQuestionState = composeQuestionState(currentMonth, currentDay, 0), currentQuestionIndex = 0, answerState = List(MAX_QUESTIONS) { false })
        _gameState.postValue(Resource.Success(currentState))
        persistState()
    }

    private fun composeQuestionState(month: Int, day: Int, index: Int): QuestionState {
        val random = Random(month * 100 + day)

        val eventList = events.toMutableList()
        eventList.shuffle(random)
        val event = eventList[index % eventList.size]

        val yearChoices = mutableListOf<Int>()
        var curYear = event.year
        var minYear = event.year
        var maxYear = event.year
        yearChoices.add(event.year)

        repeat(3) {
            var diff = abs(random.nextInt() % 20)
            if (diff == 0) diff = 1
            if (diff > 0) {
                if (maxYear + diff > currentDate.year) {
                    minYear -= diff
                    curYear = minYear
                } else {
                    maxYear += diff
                    curYear = maxYear
                }
            } else {
                minYear -= diff
                curYear = minYear
            }
            yearChoices.add(curYear)
        }

        return QuestionState(event, yearChoices.shuffled(), month, day)
    }

    private fun persistState() {
        Prefs.otdGameState = JsonUtil.encodeToString(currentState).orEmpty()
    }

    @Serializable
    data class GameState(
        val totalQuestions: Int = Prefs.otdGameQuestionsPerDay,
        val currentQuestionIndex: Int = 0,

        // history of today's answers (correct vs incorrect)
        val answerState: List<Boolean> = List(MAX_QUESTIONS) { false },

        // map of:   year: month: day: list of answers
        val answerStateHistory: Map<Int, Map<Int, Map<Int, List<Boolean>>>> = emptyMap(),

        val currentQuestionState: QuestionState
    )

    @Serializable
    data class QuestionState(
        val event: OnThisDay.Event,
        val yearChoices: List<Int>,
        val month: Int = 0,
        val day: Int = 0,
        val yearSelected: Int? = null,
        val goToNext: Boolean = false
    )

    class CurrentQuestionCorrect(val data: GameState) : Resource<GameState>()
    class CurrentQuestionIncorrect(val data: GameState) : Resource<GameState>()
    class GameStarted(val data: GameState) : Resource<GameState>()
    class GameEnded(val data: GameState) : Resource<GameState>()

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnThisDayGameViewModel(bundle) as T
        }
    }

    companion object {
        const val MAX_QUESTIONS = 10

        fun daysLeft(): String {
            val daysLeft = DateUtil.getDayDifferenceString(Date(), DateUtil.dbDateParse("20240901000000"))
            return daysLeft
        }

        // TODO: needs to verify the date logic is accurate
        fun showDialogOrIndicator(): Boolean {
            if (Prefs.lastOtdGameVisitDate.isEmpty()) {
                return true
            }
            val newUTCDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant())
            val dayDifference = DateUtil.getDayDifferenceString(newUTCDate, DateUtil.dbDateParse(Prefs.lastOtdGameVisitDate)).toInt()
            return dayDifference > 0
        }

        val gameStartDate: LocalDate get() {
            return try {
                LocalDate.parse(Prefs.otdGameStartDate)
            } catch (e: Exception) {
                LocalDate.ofEpochDay(0)
            }
        }

        val gameEndDate: LocalDate get() {
            return try {
                LocalDate.parse(Prefs.otdGameEndDate)
            } catch (e: Exception) {
                LocalDate.ofEpochDay(0)
            }
        }
    }
}
