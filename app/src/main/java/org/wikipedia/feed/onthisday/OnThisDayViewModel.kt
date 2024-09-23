package org.wikipedia.feed.onthisday

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelable
import org.wikipedia.extensions.serializable
import org.wikipedia.util.DateUtil
import org.wikipedia.util.Resource
import java.util.Calendar

class OnThisDayViewModel(bundle: Bundle) : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    val wikiSite = bundle.parcelable<WikiSite>(Constants.ARG_WIKISITE)!!
    val age = bundle.getInt(OnThisDayActivity.EXTRA_AGE, 0)
    val year = bundle.getInt(OnThisDayActivity.EXTRA_YEAR, 0)
    val invokeSource = bundle.serializable<Constants.InvokeSource>(Constants.INTENT_EXTRA_INVOKE_SOURCE)
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

    class Factory(private val bundle: Bundle) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnThisDayViewModel(bundle) as T
        }
    }
}
