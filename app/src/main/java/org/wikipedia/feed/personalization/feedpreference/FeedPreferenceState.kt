package org.wikipedia.feed.personalization.feedpreference

import org.wikipedia.R

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
