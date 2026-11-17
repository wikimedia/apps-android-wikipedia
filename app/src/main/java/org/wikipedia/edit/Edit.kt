package org.wikipedia.edit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwPostResponse

@Serializable
class Edit : MwPostResponse() {

    val edit: Result? = null

    @Serializable
    class Result {
        private val captcha: Captcha? = null
        @SerialName("result")
        val status: String? = null
        @SerialName("newrevid")
        val newRevId: Long = 0
        val code: String? = null
        val info: String? = null
        val warning: String? = null
        val spamblacklist: String? = null

        val editSucceeded get() = "Success" == status

        val hasHCaptchaResponse get() = captcha?.type == "hcaptcha" && !captcha.key.isNullOrEmpty()
        val hCaptchaKey get() = captcha?.key.orEmpty()

        val hasCaptchaResponse get() = captcha != null
        val captchaId get() = captcha?.id.orEmpty()

        val hasEditErrorCode get() = code != null
        val hasSpamBlacklistResponse get() = spamblacklist != null
    }

    @Serializable
    private class Captcha {
        val type: String? = null
        val key: String? = null
        val error: String? = null
        // Legacy FancyCaptcha ID:
        val id: String? = null
    }
}
