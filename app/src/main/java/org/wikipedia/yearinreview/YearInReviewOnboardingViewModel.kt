package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L

class YearInReviewOnboardingViewModel : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.value = UiState.Error(throwable)
    }
    private var _uiState = MutableStateFlow<UiState<Boolean>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        assertLoggedIn()
    }

    fun assertLoggedIn() {
        viewModelScope.launch(handler) {
            _uiState.value = UiState.Loading
            if (AccountUtil.isLoggedIn) {
                // Make a call to get user info, which will assert the user is logged in.
                // (If the user is no longer logged in, it will throw.)
                ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
            }
            _uiState.value = UiState.Success(AccountUtil.isLoggedIn)
        }
    }
}
