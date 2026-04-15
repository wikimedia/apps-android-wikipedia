package org.wikipedia.feed.personalization.interest

import org.wikipedia.page.PageTitle

data class OnboardingTopic(
    val topicId: String,
    val msgKey: String,
    val queryTopicId: String,
    val displayTitle: String,
    val isSelected: Boolean = false
)

data class InterestUiState(
    val topicsState: TopicsState = TopicsState.Loading,
    val articlesState: ArticlesState = ArticlesState.Loading,
    val totalSelectedCount: Int = 0
)

sealed interface TopicsState {
    data object Loading : TopicsState
    data class Success(val topics: List<OnboardingTopic>) : TopicsState
    data class Error(val message: Throwable) : TopicsState
}

sealed interface ArticlesState {
    data object Loading : ArticlesState
    data class Success(val articles: List<PageTitle>, val selectedArticles: Set<PageTitle>) : ArticlesState
    data class Error(val message: Throwable) : ArticlesState
}
