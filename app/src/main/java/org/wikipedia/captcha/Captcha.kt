package org.wikipedia.captcha

import org.wikipedia.dataclient.mwapi.MwResponse

data class Captcha(private val fancycaptchareload: FancyCaptchaReload) : MwResponse() {
    fun captchaId(): String {
        return fancycaptchareload.index.orEmpty()
    }

    data class FancyCaptchaReload(val index: String?)
}
