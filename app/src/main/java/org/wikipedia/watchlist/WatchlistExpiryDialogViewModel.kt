package org.wikipedia.watchlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class WatchlistExpiryDialogViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    var pageTitle = savedStateHandle.get<PageTitle>(WatchlistExpiryDialog.ARG_PAGE_TITLE)!!
    var expiry = savedStateHandle.get<WatchlistExpiry>(WatchlistExpiryDialog.ARG_EXPIRY)!!

    private val _uiState = MutableStateFlow(Resource<WatchlistExpiry>())
    val uiState = _uiState.asStateFlow()

    fun changeExpiry(expiry: WatchlistExpiry) {
        WatchlistAnalyticsHelper.logAddedToWatchlist(pageTitle)
        viewModelScope.launch(handler) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getWatchToken().query?.watchToken()
            val response = ServiceFactory.get(pageTitle.wikiSite)
                .watch(null, null, pageTitle.prefixedText, expiry.expiry, token!!)
            response.getFirst()?.let {
                WatchlistAnalyticsHelper.logAddedToWatchlistSuccess(pageTitle)
                _uiState.value = Resource.Success(expiry)
            }
        }
    }
}
