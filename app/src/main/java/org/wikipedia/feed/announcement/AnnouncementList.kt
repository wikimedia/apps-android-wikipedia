package org.wikipedia.feed.announcement

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AnnouncementList(@Json(name = "announce") val items: List<Announcement> = emptyList())
