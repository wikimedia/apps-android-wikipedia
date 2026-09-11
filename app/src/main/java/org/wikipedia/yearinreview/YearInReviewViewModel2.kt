package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.util.log.L

// TODO: change the name of this class to YearInReviewViewModel once the old one is removed
class YearInReviewViewModel2(
    private val repository: YearInReviewRepository = YearInReviewRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<YearInReviewUiState>(YearInReviewUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.value = YearInReviewUiState.Error(throwable)
    }

    init {
        loadYearInReview()
    }

    fun loadYearInReview() {
        _uiState.value = YearInReviewUiState.Loading
        viewModelScope.launch(exceptionHandler) {
            val yearInReview = repository.getYearInReview(YearInReviewViewModel.YIR_YEAR)
            _uiState.value = YearInReviewUiState.Content(
                year = yearInReview.year,
                pages = listOf(YearInReviewPage.ReadingDays(id = "reading_days"), YearInReviewPage.ReadingDays(id = "reading_days_2")), // TODO: Populate actual pages
                isDonationEligible = yearInReview.isDonationEligible
            )
        }
    }
}

data class YearInReviewSnapshot(
    val year: Int,
    val isDonationEligible: Boolean
)

sealed interface YearInReviewUiState {
    object Loading : YearInReviewUiState
    data class Content(
        val year: Int,
        val isDonationEligible: Boolean,
        val pages: List<YearInReviewPage>,
    ) : YearInReviewUiState
    data class Error(val error: Throwable) : YearInReviewUiState
}

sealed interface YearInReviewPage {
    val id: String

    data class ReadingDays(
        override val id: String
    ) : YearInReviewPage
}
