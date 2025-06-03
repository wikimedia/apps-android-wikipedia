package org.wikipedia.readinglist.recommended

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.SingleLiveData

class RecommendedReadingListInterestsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    val fromSettings = savedStateHandle.get<Boolean>(Constants.ARG_BOOLEAN) == true

    private val _uiState = MutableStateFlow(Resource<UiState>())
    val uiState: StateFlow<Resource<UiState>> = _uiState.asStateFlow()

    val historyItems = MutableLiveData(Resource<List<Any>>())

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            _uiState.value = Resource.Error(throwable)
        }) {
            _uiState.value = Resource.Loading()



            delay(1000)



            _uiState.value = Resource.Success(
                UiState(
                    fromSettings = fromSettings
                )
            )
        }
    }

    data class UiState(
        val fromSettings: Boolean = false,
        val items: List<PageTitle> = emptyList()
    )
}
