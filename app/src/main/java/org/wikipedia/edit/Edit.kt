package org.wikipedia.edit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwPostResponse
import org.wikipedia.dataclient.mwapi.MwServiceError

@JsonClass(generateAdapter = true)
class Edit(
    errors: List<MwServiceError> = emptyList(),
    @Json(name = "servedby") servedBy: String = "",
    val edit: Result? = null
) : MwPostResponse(errors, servedBy) {
    @JsonClass(generateAdapter = true)
    class Result(
        internal val captcha: Captcha? = null,
        @Json(name = "result") val status: String? = null,
        @Json(name = "newrevid") val newRevId: Long = 0,
        val code: String? = null,
        val info: String? = null,
        val warning: String? = null,
        val spamblacklist: String? = null
    ) {
        val editSucceeded get() = "Success" == status
        val captchaId get() = captcha?.id.orEmpty()
        val hasEditErrorCode get() = code != null
        val hasCaptchaResponse get() = captcha != null
        val hasSpamBlacklistResponse get() = spamblacklist != null
    }

    @JsonClass(generateAdapter = true)
    class Captcha(val id: String? = null)
}
