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

    private val _yearInReview = MutableStateFlow<YearInReviewSnapshot?>(null)
    val yearInReview = _yearInReview.asStateFlow()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
    }

    init {
        loadYearInReview()
    }

    private fun loadYearInReview() {
        viewModelScope.launch(exceptionHandler) {
            _yearInReview.value = repository.getYearInReview(YearInReviewViewModel.YIR_YEAR)
        }
    }
}

data class YearInReviewSnapshot(
    val year: Int,
    val isDonationEligible: Boolean
)
