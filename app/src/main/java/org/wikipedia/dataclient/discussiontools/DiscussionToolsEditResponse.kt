package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DiscussionToolsEditResponse {
    @SerialName("discussiontoolsedit") val result: EditResult? = null

    @Serializable
    class EditResult(
            val result: String = "",
            val content: String = "",
            @SerialName("newrevid") val newRevId: Long = 0,
            val watched: Boolean = false
    )
}
