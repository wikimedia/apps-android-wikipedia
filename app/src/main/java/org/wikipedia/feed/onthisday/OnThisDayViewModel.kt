package org.wikipedia.feed.onthisday

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import java.util.Calendar

class OnThisDayViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    val wikiSite = savedStateHandle.get<WikiSite>(Constants.ARG_WIKISITE)!!
    val age = savedStateHandle[OnThisDayActivity.EXTRA_AGE] ?: 0
    val year = savedStateHandle[OnThisDayActivity.EXTRA_YEAR] ?: 0
    val invokeSource = savedStateHandle.get<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)!!
    val date = DateUtil.getDefaultDateFor(age)

    private val _uiState = MutableStateFlow(Resource<List<OnThisDay.Event>>())
    val uiState = _uiState.asStateFlow()

    fun loadOnThisDay(calendar: Calendar = DateUtil.getDefaultDateFor(age)) {
        _uiState.value = Resource.Loading()
        viewModelScope.launch(handler) {
            val response = ServiceFactory.getRest(wikiSite).getOnThisDay(calendar[Calendar.MONTH] + 1, calendar[Calendar.DATE])
            _uiState.value = Resource.Success(response.allEvents())
        }
    }

    private fun definitionsNotFound() {
        _uiState.value = Resource.Error(Throwable("Definitions not found."))
    }
}
