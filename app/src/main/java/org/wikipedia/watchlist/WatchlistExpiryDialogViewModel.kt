package org.wikipedia.watchlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource

class WatchlistExpiryDialogViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = Resource.Error(throwable)
    }

    var pageTitle = savedStateHandle.get<PageTitle>(WatchlistExpiryDialog.ARG_PAGE_TITLE)!!
    var expiry = savedStateHandle.get<WatchlistExpiry>(WatchlistExpiryDialog.ARG_EXPIRY)!!

    private val _uiState = MutableStateFlow(Resource<WatchlistExpiryChangeSuccess>())
    val uiState = _uiState.asStateFlow()

    fun changeExpiry(expiry: WatchlistExpiry) {
        viewModelScope.launch(handler) {
            val pair = WatchlistViewModel.watchPageTitle(this, pageTitle, false, expiry, false, pageTitle.namespace().talk())
            _uiState.value = Resource.Success(WatchlistExpiryChangeSuccess(expiry, pair.second))
        }
    }

    data class WatchlistExpiryChangeSuccess(
        val expiry: WatchlistExpiry,
        val message: String
    )
}
