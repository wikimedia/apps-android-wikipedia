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
                pages = listOf(
                    YearInReviewPage.Announcement(id = "cover", daysRead = 150),
                    // TODO: replace with real data
                    YearInReviewPage.TopicRunnersUp(
                        id = "topic_runners_up",
                        topics = listOf(
                            YearInReviewTopic(name = "Central America", articleCount = 14),
                            YearInReviewTopic(name = "Visual art", articleCount = 12),
                            YearInReviewTopic(name = "Politics and government", articleCount = 8)
                        )
                    ),
                    // TODO: replace with real data
                    YearInReviewPage.BiggestReadingDayArticles(
                        id = "biggest_reading_day_articles",
                        articles = listOf(
                            YearInReviewArticle(
                                title = "Pamela Anderson",
                                description = "Canadian-American actress and model (born 1967)",
                                thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0c/Pamela_Anderson-69699.jpg/330px-Pamela_Anderson-69699.jpg"
                            ),
                            YearInReviewArticle(
                                title = "Pamukkale",
                                description = "Natural site in Denizli Province in southwestern Turkey",
                                thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/5d/Pamukkale%2C_Denizli_2026_68.jpg/330px-Pamukkale%2C_Denizli_2026_68.jpg"
                            ),
                            YearInReviewArticle(
                                title = "Catherine, Princess of Wales",
                                description = "Member of the British royal family (born 1982)",
                                thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1a/NE_Lac_Bab_Louta_Tazekka_Nov25_A7CR_09270-4_HDR1.jpg/330px-NE_Lac_Bab_Louta_Tazekka_Nov25_A7CR_09270-4_HDR1.jpg"
                            )
                        )
                    )
                ), // TODO: Populate actual pages
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
    val useDarkStatusBarIcons: Boolean

    data class Announcement(
        override val id: String,
        override val useDarkStatusBarIcons: Boolean = false,
        val daysRead: Int
    ) : YearInReviewPage

    data class TopicRunnersUp(
        override val id: String,
        override val useDarkStatusBarIcons: Boolean = false,
        val topics: List<YearInReviewTopic>
    ) : YearInReviewPage

    data class BiggestReadingDayArticles(
        override val id: String,
        override val useDarkStatusBarIcons: Boolean = false,
        val articles: List<YearInReviewArticle>
    ) : YearInReviewPage
}

data class YearInReviewTopic(
    val name: String,
    val articleCount: Int
)

data class YearInReviewArticle(
    val title: String,
    val description: String,
    val thumbnailUrl: String?
)
