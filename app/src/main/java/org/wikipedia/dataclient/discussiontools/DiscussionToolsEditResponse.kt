package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.edit.Edit

@Serializable
class DiscussionToolsEditResponse : MwResponse() {
    @SerialName("discussiontoolsedit") val result: Edit? = null
}
