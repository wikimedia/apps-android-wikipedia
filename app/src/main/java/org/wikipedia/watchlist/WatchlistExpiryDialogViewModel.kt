package org.wikipedia.watchlist

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.analytics.eventplatform.WatchlistAnalyticsHelper
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.extensions.parcelable
import org.wikipedia.page.PageTitle

class WatchlistExpiryDialogViewModel(bundle: Bundle) : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        _uiState.value = UiState.Error(throwable)
    }

    var pageTitle = bundle.parcelable<PageTitle>(WatchlistExpiryDialog.ARG_PAGE_TITLE)!!
    var expiry = bundle.getSerializable(WatchlistExpiryDialog.ARG_EXPIRY) as WatchlistExpiry

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun changeExpiry(expiry: WatchlistExpiry) {
        WatchlistAnalyticsHelper.logAddedToWatchlist(pageTitle)
        viewModelScope.launch(handler) {
            val token = ServiceFactory.get(pageTitle.wikiSite).getWatchToken().query?.watchToken()
            val response = ServiceFactory.get(pageTitle.wikiSite)
                .watch(null, null, pageTitle.prefixedText, expiry.expiry, token!!)
            response.getFirst()?.let {
                WatchlistAnalyticsHelper.logAddedToWatchlistSuccess(pageTitle)
                _uiState.value = UiState.Success(expiry)
            }
        }
    }

    class Factory(private val bunble: Bundle) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WatchlistExpiryDialogViewModel(bunble) as T
        }
    }

    open class UiState {
        class Success(val newExpiry: WatchlistExpiry) : UiState()
        class Error(val throwable: Throwable) : UiState()
    }
}
