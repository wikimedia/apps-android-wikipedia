package org.wikipedia.feed.personalization.homepreference

import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.personalization.interest.OnboardingTopic
import org.wikipedia.page.PageTitle

enum class HomePreferenceType(val titleRes: Int) {
    COMMUNITY(R.string.explore_feed_preference_community_content_title),
    PERSONALIZED(R.string.explore_feed_preference_personalized_content_title)
}

data class HomePreferenceContent (
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val tag: String?
) {
    companion object {
        fun fromPageTitles(pageTitles: List<PageTitle>, topic: OnboardingTopic): List<HomePreferenceContent> {
            return pageTitles.map { page ->
                HomePreferenceContent(
                    title = page.displayText,
                    description = page.description,
                    imageUrl = page.thumbUrl,
                    tag = WikipediaApp.instance.getString(topic.topic.msgKey)
                )
            }
        }
    }
}

sealed interface HomeContentState {
    data object Loading : HomeContentState
    data object Empty : HomeContentState
    data class Success(val content: List<HomePreferenceContent>) : HomeContentState
    data class Error(val message: Throwable) : HomeContentState
}

data class HomePreferenceUiState(
    val selectedType: HomePreferenceType = HomePreferenceType.COMMUNITY,
    val communityState: HomeContentState = HomeContentState.Loading,
    val personalizedState: HomeContentState = HomeContentState.Loading
)
