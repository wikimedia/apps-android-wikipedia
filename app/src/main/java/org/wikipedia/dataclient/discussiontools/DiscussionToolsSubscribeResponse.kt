package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DiscussionToolsSubscribeResponse {
    @SerialName("discussiontoolssubscribe") val status: SubscribeStatus? = null

    @Serializable
    class SubscribeStatus(
            val page: String = "",
            @SerialName("commentname") val topicName: String = "",
            val subscribe: Boolean = false
    )
}
