package org.wikipedia.captcha

import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.mwapi.MwResponse

@Serializable
data class Captcha(private val fancycaptchareload: FancyCaptchaReload) : MwResponse() {
    fun captchaId(): String {
        return fancycaptchareload.index.orEmpty()
    }

    @Serializable
    data class FancyCaptchaReload(val index: String?)
}
