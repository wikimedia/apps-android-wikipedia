package org.wikipedia.games.onthisday

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.util.Resource
import java.time.LocalDate

class OnThisDayGameViewModel(bundle: Bundle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<GameState>>()
    val gameState: LiveData<Resource<GameState>> get() = _gameState

    private val currentDate = LocalDate.now()
    private val currentMonth = currentDate.monthValue
    private val currentDay = currentDate.dayOfMonth

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

            // TODO: load saved state from storage
            // otherwise, create a new state
            val state = GameState(composeQuestionState(currentMonth, currentDay, 0))

            _gameState.postValue(Resource.Success(state))
        }
    }

    fun submitCurrentResponse(choiceIndex: Int) {
        // TODO: do something if it's right or wrong
        val state = (_gameState.value as? Resource.Success<GameState>)?.data ?: return

        val nextQuestionIndex = state.currentQuestionIndex + 1

        val newState = state.copy(currentQuestionState = composeQuestionState(nextQuestionIndex, currentMonth, currentDay), currentQuestionIndex = nextQuestionIndex)
        _gameState.postValue(Resource.Success(newState))
    }

    private fun composeQuestionState(month: Int, day: Int, index: Int): QuestionState {
        val event = events[index]
        val yearChoices = mutableListOf<Int>().apply {
            add(event.year)
            repeat(3) {
                add((event.year - 10..event.year + 10).random())
            }
        }
        return QuestionState(event, yearChoices.shuffled())
    }

    data class GameState(
        val currentQuestionState: QuestionState,

        // TODO: everything below this line should be persisted

        val totalQuestions: Int = NUM_QUESTIONS,
        val currentQuestionIndex: Int = 0,

        // history of today's answers (correct vs incorrect)
        val answerState: List<Boolean> = emptyList(),

        // map of:   year: month: day: list of answers
        val answerStateHistory: Map<Int, Map<Int, Map<Int, List<Boolean>>>> = emptyMap()
    )

    class QuestionState(
        val event: OnThisDay.Event,
        val yearChoices: List<Int>
    )

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnThisDayGameViewModel(bundle) as T
        }
    }

    companion object {
        const val NUM_QUESTIONS = 3
    }
}
