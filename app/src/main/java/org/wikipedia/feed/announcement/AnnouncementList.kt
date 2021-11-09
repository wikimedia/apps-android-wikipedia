package org.wikipedia.feed.announcement

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AnnouncementList {

    @SerialName("announce")
    val items: List<Announcement> = emptyList()
}
