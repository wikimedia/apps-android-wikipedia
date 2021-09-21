package org.wikipedia.feed.announcement

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AnnouncementList {

    @SerialName("announce")@SerializedName("announce")
    val items: List<Announcement> = emptyList()
}
