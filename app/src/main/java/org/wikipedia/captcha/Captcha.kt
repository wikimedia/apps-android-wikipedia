package org.wikipedia.captcha

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse

@Serializable
data class Captcha(@SerializedName("fancycaptchareload") private val fancyCaptchaReload: FancyCaptchaReload) : MwResponse() {
    fun captchaId(): String {
        return fancyCaptchaReload.index.orEmpty()
    }

    @Serializable
    data class FancyCaptchaReload(val index: String?)
}
