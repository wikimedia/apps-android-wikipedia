package org.wikipedia.feed.personalization.interest

import org.wikipedia.page.PageTitle
import org.wikipedia.topics.ArticleTopic

data class OnboardingTopic(
    val topic: ArticleTopic,
    val isSelected: Boolean = false
)

data class InterestUiState(
    val topicsList: List<OnboardingTopic> = emptyList(),
    val articlesState: ArticlesState = ArticlesState.Loading,
    val totalSelectedCount: Int = 0,
    val languageCode: String
)

sealed interface ArticlesState {
    data object Loading : ArticlesState
    data class Success(val articles: List<PageTitle>, val selectedArticles: Set<PageTitle>) : ArticlesState
    data class Error(val message: Throwable) : ArticlesState
}
