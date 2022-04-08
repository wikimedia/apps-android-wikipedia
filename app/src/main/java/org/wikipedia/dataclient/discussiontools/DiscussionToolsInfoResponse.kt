package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DiscussionToolsInfoResponse {
    @SerialName("discussiontoolspageinfo") val pageInfo: PageInfo? = null

    @Serializable
    class PageInfo(
            @SerialName("threaditemshtml") val threads: List<ThreadItem> = emptyList()
    )
}
