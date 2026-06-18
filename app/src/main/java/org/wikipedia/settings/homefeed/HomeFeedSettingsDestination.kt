package org.wikipedia.settings.homefeed

import kotlinx.serialization.Serializable

sealed interface HomeFeedSettingsDestination {
    @Serializable
    data object Root : HomeFeedSettingsDestination
    @Serializable
    data object ForYouModuleScreen : HomeFeedSettingsDestination
    @Serializable
    data object CommunityModuleScreen : HomeFeedSettingsDestination
    @Serializable
    data object FeedConfiguration : HomeFeedSettingsDestination
    @Serializable
    data object DefaultFeedViewScreen : HomeFeedSettingsDestination
}
