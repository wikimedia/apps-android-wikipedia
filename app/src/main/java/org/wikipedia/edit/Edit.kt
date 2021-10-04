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
        val captchaId get() = captcha?.id.orEmpty()
        val hasEditErrorCode get() = code != null
        val hasCaptchaResponse get() = captcha != null
        val hasSpamBlacklistResponse get() = spamblacklist != null
    }

    @Serializable
    private class Captcha {
        val id: String? = null
    }
}
