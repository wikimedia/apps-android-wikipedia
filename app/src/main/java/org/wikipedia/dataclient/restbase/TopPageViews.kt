package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class TopPageViews {
    val items: List<Item> = emptyList()

    @Serializable
    class Item {
        val articles: List<Article> = emptyList()
    }

    @Serializable
    class Article(
        val article: String = "",
        val views: Long = 0,
        val rank: Int = 0
    )
}
