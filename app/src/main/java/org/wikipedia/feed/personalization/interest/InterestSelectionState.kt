package org.wikipedia.feed.personalization.interest

import org.wikipedia.R
import org.wikipedia.page.PageTitle

// Interest Selection screen state
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

// Feed Preference screen state
enum class FeedPreferenceType(val titleRes: Int) {
    COMMUNITY(R.string.explore_feed_preference_community_content_title),
    PERSONALIZED(R.string.explore_feed_preference_personalized_content_title)
}

data class FeedPreferenceContent (
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val tag: String
)

sealed interface FeedContentState {
    data object Loading : FeedContentState
    data class Success(val content: List<FeedPreferenceContent>) : FeedContentState
    data class Error(val message: Throwable) : FeedContentState
}

data class FeedPreferenceUiState(
    val selectedType: FeedPreferenceType = FeedPreferenceType.COMMUNITY,
    val communityState: FeedContentState = FeedContentState.Loading,
    val personalizedState: FeedContentState = FeedContentState.Loading
)
