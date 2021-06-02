package org.wikipedia.edit

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.mwapi.MwPostResponse

class Edit : MwPostResponse() {

    val edit: Result? = null

    class Result {
        private val captcha: Captcha? = null
        @SerializedName("result")
        val status: String? = null
        @SerializedName("newrevid")
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

    private class Captcha {
        val id: String? = null
    }
}
