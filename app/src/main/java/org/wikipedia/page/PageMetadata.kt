package org.wikipedia.page

import kotlinx.serialization.Serializable

@Serializable
class PageMetadata(
    val ns: Int = 0,
    val pageId: Long = 0,
    val modified: String? = null,
    val leadImage: LeadImage? = null,
    val topics: List<Topic> = emptyList()
) {
    @Serializable
    class Topic(
        val topic: String? = null,
        val score: Double = 0.0
    )

    @Serializable
    class LeadImage(
        val source: String? = null,
        val width: Int = 0,
        val height: Int = 0
    )
}
