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
            val watched: Boolean = false,
            val edit: EditFailure? = null
    ) {
        val captcha get() = edit?.captcha
    }

    @Serializable
    class EditFailure(val captcha: Captcha? = null)

    @Serializable
    class Captcha(
            val type: String = "",
            @SerialName("key") val siteKey: String = "",
            val error: String? = null
    ) {
        val isHCaptcha get() = type == "hcaptcha"

        // Forced challenge (e.g. AbuseFilter); the resubmit must echo wgConfirmEditForceShowCaptcha.
        val forceShowCaptcha get() = error == "forceshowcaptcha"
    }
}
