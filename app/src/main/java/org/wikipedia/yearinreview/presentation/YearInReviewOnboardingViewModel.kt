package org.wikipedia.yearinreview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.util.UiState
import org.wikipedia.util.log.L
import org.wikipedia.yearinreview.data.YearInReviewRepository
import org.wikipedia.yearinreview.data.YearInReviewRepositoryImpl
import org.wikipedia.yearinreview.data.YearInReviewSnapshot

class YearInReviewOnboardingViewModel : ViewModel() {
    private val handler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.value = UiState.Error(throwable)
    }
    private var _uiState = MutableStateFlow<UiState<Pair<Boolean, YearInReviewSnapshot>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val repository: YearInReviewRepository = YearInReviewRepositoryImpl()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch(handler) {
            _uiState.value = UiState.Loading
            val snapshot = async { repository.getYearInReview() }

            if (AccountUtil.isLoggedIn) {
                // Make a call to get user info, which will assert the user is logged in.
                // (If the user is no longer logged in, it will throw.)
                ServiceFactory.get(WikipediaApp.instance.wikiSite).getUserInfo()
            }
            _uiState.value = UiState.Success(Pair(AccountUtil.isLoggedIn, snapshot.await()))
        }
    }
}
