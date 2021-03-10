package org.wikipedia.captcha

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.edit.EditResult

// Handles only Image Captchas
class CaptchaResult(val captchaId: String) : EditResult("Failure") {
    fun getCaptchaUrl(wiki: WikiSite): String {
        return wiki.url("index.php") + "?title=Special:Captcha/image&wpCaptchaId=" + captchaId
    }
}
