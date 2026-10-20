package org.wikipedia.yearinreview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.settings.dev.playground.YearInReviewPlayground
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.log.L
import org.wikipedia.yearinreview.data.YearInReviewAvailability
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewDonationEligibility
import org.wikipedia.yearinreview.data.YearInReviewRepository
import org.wikipedia.yearinreview.data.YearInReviewRepositoryImpl

sealed interface YearInReviewUiState {
    object Loading : YearInReviewUiState
    data class Content(
        val year: Int,
        val isPersonalized: Boolean,
        val isDonationEligible: Boolean,
        val pages: List<YearInReviewPage>,
    ) : YearInReviewUiState
    data class Error(val error: Throwable) : YearInReviewUiState
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
            val yearInReview = YearInReviewPlayground.snapshot ?: repository.getYearInReview()
            _uiState.value = YearInReviewUiState.Content(
                year = yearInReview.year,
                isPersonalized = YearInReviewFlowDecider.hasEnoughPersonalizedPages(yearInReview),
                pages = YearInReviewPlayground.pages ?: YearInReviewFlowDecider.pages(yearInReview),
                isDonationEligible = yearInReview.isDonationEligible
            )
        }
    }

    companion object {
        const val YIR_YEAR = YearInReviewConfig.YEAR
        const val YIR_TAG = "yir_$YIR_YEAR"

        // Whether Year-in-Review should be accessible at all.
        // (different from the user enabling/disabling it in Settings.)
        val isAccessible get(): Boolean {
            return YearInReviewAvailability(YIR_YEAR).isAvailable(
                remoteConfig = RemoteConfig.config,
                countryCode = GeoUtil.geoIPCountry,
                developerOverride = Prefs.isShowDeveloperSettingsEnabled
            )
        }

        val canShowEntryPoint get() = YearInReviewPlayground.canShowEntryPoint ?: YearInReviewAvailability(YIR_YEAR).canShowEntryPoint(
            remoteConfig = RemoteConfig.config,
            countryCode = GeoUtil.geoIPCountry,
            isEnabled = Prefs.isYearInReviewEnabled,
            developerOverride = Prefs.isShowDeveloperSettingsEnabled
        )

        var currentCampaignId: String? = null

        val isCustomIconAllowed get() = Prefs.yearInReviewCachedStats[YIR_YEAR]?.let {
            YearInReviewDonationEligibility().hasDonatedWithinContributionsDateRange(YearInReviewConfig.cachedRemoteConfig) ||
                    (it.editingStats?.totalEditCount ?: 0) > 0
        } == true
    }
}
