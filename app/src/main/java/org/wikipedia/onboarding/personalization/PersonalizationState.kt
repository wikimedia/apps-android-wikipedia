package org.wikipedia.onboarding.personalization

import org.wikipedia.page.PageTitle

// TODO: update the states below as needed as we build out the screen
data class OnboardingTopic(
    val topicId: String,
    val msgKey: String,
    val articleTopics: String,
    val displayTitle: String,
    val isSelected: Boolean = false
)

data class InterestUiState(
    val topicsState: TopicsState = TopicsState.Loading,
    val articlesState: ArticlesState = ArticlesState.Loading
)

sealed interface TopicsState {
    data object Loading : TopicsState
    data class Success(val topics: List<OnboardingTopic>) : TopicsState
    data class Error(val message: String) : TopicsState
}

sealed interface ArticlesState {
    data object Loading : ArticlesState
    data class Success(val articles: List<PageTitle>, val selectedArticles: Set<PageTitle>) : ArticlesState
    data class Error(val message: String) : ArticlesState
}
