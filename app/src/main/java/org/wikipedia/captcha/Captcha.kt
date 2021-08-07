package org.wikipedia.captcha

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.mwapi.MwResponse

@JsonClass(generateAdapter = true)
data class Captcha(@Json(name = "fancycaptchareload") internal val fancyCaptchaReload: FancyCaptchaReload) : MwResponse() {
    fun captchaId(): String {
        return fancyCaptchaReload.index.orEmpty()
    }

    @JsonClass(generateAdapter = true)
    data class FancyCaptchaReload(val index: String?)
}
