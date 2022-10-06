package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse

@Serializable
class DiscussionToolsEditResponse : MwResponse() {
    @SerialName("discussiontoolsedit") val result: EditResult? = null

    @Serializable
    class EditResult(
            val result: String = "",
            val content: String = "",
            @SerialName("newrevid") val newRevId: Long = 0,
            val watched: Boolean = false
    )
}
