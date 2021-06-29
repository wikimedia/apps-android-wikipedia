package org.wikipedia.feed.announcement

import com.google.gson.annotations.SerializedName

class AnnouncementList {

    @SerializedName("announce")
    val items: List<Announcement> = emptyList()
}
