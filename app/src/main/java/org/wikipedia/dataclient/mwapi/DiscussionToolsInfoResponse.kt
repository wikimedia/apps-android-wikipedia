package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DiscussionToolsInfoResponse {
    @SerialName("discussiontoolspageinfo") val pageInfo: PageInfo? = null

    @Serializable
    class PageInfo(
            @SerialName("threaditemshtml") val threads: List<ThreadItem> = emptyList()
    )

    @Serializable
    class ThreadItem(
            val type: String = "",
            val level: Int = 0,
            val id: String = "",
            val html: String = "",
            val author: String = "",
            val timestamp: String = "",
            val headingLevel: Int = 0,
            val placeholderHeading: Boolean = false,
            val replies: List<ThreadItem> = emptyList()
    )
}
