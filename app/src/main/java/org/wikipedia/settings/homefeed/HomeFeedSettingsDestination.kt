package org.wikipedia.settings.homefeed

import kotlinx.serialization.Serializable

sealed interface HomeFeedSettingsDestination {
    @Serializable
    data object Root : HomeFeedSettingsDestination
    @Serializable
    data object ForYouModules : HomeFeedSettingsDestination
    @Serializable
    data object CommunityModules : HomeFeedSettingsDestination
    @Serializable
    data object WhatsDrivingFeedModules : HomeFeedSettingsDestination
}
