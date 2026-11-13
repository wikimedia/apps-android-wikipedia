package org.wikipedia.yearinreview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.settings.dev.playground.YearInReviewPlayground
import org.wikipedia.util.GeoUtil
import org.wikipedia.util.log.L
import org.wikipedia.yearinreview.data.YearInReviewAvailability
import org.wikipedia.yearinreview.data.YearInReviewConfig
import org.wikipedia.yearinreview.data.YearInReviewReadingStats
import org.wikipedia.yearinreview.data.YearInReviewRepository
import org.wikipedia.yearinreview.data.YearInReviewRepositoryImpl

sealed interface YearInReviewUiState {
    object Loading : YearInReviewUiState
    data class Content(
        val year: Int,
        val isPersonalized: Boolean,
        val isDonationEligible: Boolean,
        val canShareHighlights: Boolean,
        val pages: List<YearInReviewPage>,
        val readingStats: YearInReviewReadingStats
    ) : YearInReviewUiState
    data class Error(val error: Throwable) : YearInReviewUiState
}

class YearInReviewViewModel(
    private val repository: YearInReviewRepository = YearInReviewRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<YearInReviewUiState>(YearInReviewUiState.Loading)
    val uiState = _uiState.asStateFlow()
    private var isOnEndPage = false
    private var hasLogoSoundPlayed = false
    private val audioPlayer = YearInReviewAudioPlayer(
        context = WikipediaApp.instance,
        onPlaybackEnded = { resourceId ->
            if (resourceId == R.raw.wikipedia_logo_sound) {
                hasLogoSoundPlayed = true
            }
        }
    )
    val isSoundOn = MutableStateFlow(Prefs.isYearInReviewSoundOn)

    val tabArticles: List<PageTitle> = loadTabArticles()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        L.e(throwable)
        _uiState.value = YearInReviewUiState.Error(throwable)
    }

    init {
        loadYearInReview()
    }

    private fun loadTabArticles(): List<PageTitle> {
        val titles = WikipediaApp.instance.tabList.asReversed()
            .asSequence()
            .mapNotNull { it.backStackPositionTitle }
            .filter { it.namespace() == Namespace.MAIN && !it.isMainPage }
            .distinct()
            .take(MAX_OPEN_TABS)
            .map { it.copy() }
            .toList()
        return titles.takeIf { it.size >= MIN_OPEN_TABS }.orEmpty()
    }

    fun loadYearInReview() {
        _uiState.value = YearInReviewUiState.Loading
        viewModelScope.launch(exceptionHandler) {
            val yearInReview = YearInReviewPlayground.snapshot ?: repository.getYearInReview()
            _uiState.value = YearInReviewUiState.Content(
                year = yearInReview.year,
                isPersonalized = yearInReview.isLoggedIn && YearInReviewFlowDecider.hasEnoughPersonalizedPages(yearInReview),
                pages = YearInReviewPlayground.pages ?: YearInReviewFlowDecider.pages(yearInReview),
                isDonationEligible = yearInReview.isDonationEligible,
                canShareHighlights = YearInReviewFlowDecider.canShareHighlights(yearInReview),
                readingStats = yearInReview.readingStats
            )
        }
    }

    fun updateAudio() {
        when {
            !isSoundOn.value || hasLogoSoundPlayed -> audioPlayer.pause()
            isOnEndPage -> audioPlayer.play(R.raw.wikipedia_logo_sound, loop = false)
            else -> audioPlayer.play(R.raw.dreamy, loop = true)
        }
    }

    fun onPageSettled(page: YearInReviewPage) {
        isOnEndPage = page is YearInReviewPage.End
        if (isOnEndPage) {
            updateAudio()
        }
    }

    fun pauseAudio() {
        audioPlayer.pause()
    }

    fun toggleSound() {
        isSoundOn.value = !isSoundOn.value
        Prefs.isYearInReviewSoundOn = isSoundOn.value
        updateAudio()
    }

    override fun onCleared() {
        audioPlayer.release()
    }

    companion object {
        private const val MIN_OPEN_TABS = 2
        private const val MAX_OPEN_TABS = 10

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
    }
}
