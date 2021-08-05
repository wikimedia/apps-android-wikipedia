package org.wikipedia.feed.announcement

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
class AnnouncementList {

    @SerializedName("announce")
    val items: List<Announcement> = emptyList()
}
