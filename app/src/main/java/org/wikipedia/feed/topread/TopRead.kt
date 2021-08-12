package org.wikipedia.feed.topread

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TopRead {

    val date: @Contextual Date = Date()

    val articles: List<TopReadArticles> = emptyList()
}
