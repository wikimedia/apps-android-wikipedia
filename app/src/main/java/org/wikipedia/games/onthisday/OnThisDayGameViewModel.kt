package org.wikipedia.games.onthisday

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wikipedia.util.Resource

class OnThisDayGameViewModel(bundle: Bundle) : ViewModel() {

    private val _gameState = MutableLiveData<Resource<GameState>>()
    val gameState: LiveData<Resource<GameState>> get() = _gameState

    init {
        loadGameState()
    }

    fun loadGameState() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _gameState.postValue(Resource.Error(throwable))
        }) {
            _gameState.postValue(Resource.Loading())

            delay(3000)

            val state = GameState()

            _gameState.postValue(Resource.Success(state))
        }
    }

    class GameState {

    }

    class Factory(val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnThisDayGameViewModel(bundle) as T
        }
    }
}
