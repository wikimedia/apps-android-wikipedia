package org.wikipedia.feed.topread

import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
class TopRead(val date: Date = Date(), val articles: List<TopReadArticles> = emptyList())
