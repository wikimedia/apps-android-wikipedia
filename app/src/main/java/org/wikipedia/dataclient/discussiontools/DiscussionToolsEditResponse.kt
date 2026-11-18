package org.wikipedia.dataclient.discussiontools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse
import org.wikipedia.edit.Edit

@Serializable
class DiscussionToolsEditResponse : MwResponse() {
    @SerialName("discussiontoolsedit") val result: EditResult? = null

    @Serializable
    class EditResult(
            val result: String = "",
            val content: String = "",
            @SerialName("newrevid") val newRevId: Long = 0,
            val watched: Boolean = false,
            // Only present when the edit was not saved, e.g. when a captcha is required.
            val edit: Edit.Result? = null
    ) {
        val hasHCaptchaResponse get() = edit?.hasHCaptchaResponse == true
    }
}
