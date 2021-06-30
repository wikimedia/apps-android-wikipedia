package org.wikipedia.feed.topread

import org.wikipedia.json.annotations.Required
import java.util.*

class TopRead {

    @Required
    val date: Date? = null

    @Required
    val articles: List<TopReadArticles>? = null
}
