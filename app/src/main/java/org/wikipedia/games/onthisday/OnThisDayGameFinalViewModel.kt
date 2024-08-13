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
import org.wikipedia.json.JsonUtil
import org.wikipedia.settings.Prefs
import org.wikipedia.util.Resource
import java.time.LocalDate

class OnThisDayGameFinalViewModel(bundle: Bundle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<OnThisDayGameViewModel.GameState>>()
    val gameState: LiveData<Resource<OnThisDayGameViewModel.GameState>> get() = _gameState

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

            _gameState.postValue(Resource.Success(JsonUtil.decodeFromString<OnThisDayGameViewModel.GameState>(Prefs.otdGameState)!!))
        }
    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnThisDayGameFinalViewModel(bundle) as T
        }
    }
}
