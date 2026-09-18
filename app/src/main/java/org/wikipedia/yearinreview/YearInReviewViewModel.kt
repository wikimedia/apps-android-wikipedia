package org.wikipedia.yearinreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.log.L
import java.time.LocalDateTime

data class YearInReviewSnapshot(
    val year: Int,
    val isDonationEligible: Boolean,
    val remoteConfig: RemoteConfig.RemoteConfigYearInReview? = null,
    val readingStats: YearInReviewReadingStats? = null,
    val editingStats: YearInReviewEditingStats? = null,
    val rewardData: YearInReviewRewardData
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

class YearInReviewViewModel(
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
            val yearInReview = repository.getYearInReview(YIR_YEAR)
            _uiState.value = YearInReviewUiState.Content(
                year = yearInReview.year,
                pages = listOf(YearInReviewPage.ReadingDays(id = "reading_days"), YearInReviewPage.ReadingDays(id = "reading_days_2")), // TODO: Populate actual pages
                isDonationEligible = yearInReview.isDonationEligible
            )
        }
    }

    companion object {
        const val YIR_YEAR = 2025
        const val YIR_TAG = "yir_$YIR_YEAR"
        const val MAX_EDITED_TIMES = 500
        const val MIN_SAVED_ARTICLES = 3
        const val MIN_TOP_CATEGORY = 3
        const val MIN_READING_ARTICLES = 5
        const val MIN_READING_MINUTES = 1
        const val MIN_ARTICLES_PER_MAP_CLUSTER = 2
        const val MAX_ARTICLES_ON_MAP = 32

        // Whether Year-in-Review should be accessible at all.
        // (different from the user enabling/disabling it in Settings.)
        val isAccessible get(): Boolean {
            if (Prefs.isShowDeveloperSettingsEnabled) {
                return true
            }
            val config = RemoteConfig.config.commonv1?.getYirForYear(YIR_YEAR)
            val now = LocalDateTime.now()
            return (config != null &&
                    !config.hideCountryCodes.contains(GeoUtil.geoIPCountry) &&
                    now.isAfter(config.activeStartDate) &&
                    now.isBefore(config.activeEndDate))
        }

        var currentCampaignId: String? = null

        val isCustomIconAllowed get() = Prefs.yearInReviewCachedStats[YIR_YEAR]?.let {
            Prefs.donationResults.isNotEmpty() || (it.editingStats?.userEditsCount ?: 0) > 0
        } == true

        fun updateYearInReviewModel(year: Int = YIR_YEAR, update: (YearInReviewModel) -> YearInReviewModel) {
            val currentData = Prefs.yearInReviewModelData.toMutableMap()
            currentData[year]?.let { model ->
                currentData[year] = update(model)
                Prefs.yearInReviewModelData = currentData
            }
        }

        fun getYearInReviewModel(year: Int = YIR_YEAR): YearInReviewModel? {
            return Prefs.yearInReviewModelData[year]
        }
    }
}
