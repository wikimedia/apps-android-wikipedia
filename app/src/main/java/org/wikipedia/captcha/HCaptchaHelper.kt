package org.wikipedia.captcha

import androidx.fragment.app.FragmentActivity
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaTheme
import com.hcaptcha.sdk.HCaptchaTokenResponse
import org.wikipedia.WikipediaApp
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L

class HCaptchaHelper(
    private val activity: FragmentActivity,
    private val callback: Callback
) {
    fun interface Callback {
        fun onSuccess(token: String)
    }

    private var hCaptcha: HCaptcha? = null
    private var tokenResponse: HCaptchaTokenResponse? = null


    fun show() {
        if (hCaptcha == null) {
            hCaptcha = HCaptcha.getClient(activity)
            hCaptcha?.setup(
                HCaptchaConfig.builder()
                    .siteKey("f1f21d64-6384-4114-b7d0-d9d23e203b4a")
                    .theme(if (WikipediaApp.instance.currentTheme.isDark) HCaptchaTheme.DARK else HCaptchaTheme.LIGHT)
                    .host("meta.wikimedia.org")

                    .jsSrc("https://assets-hcaptcha.wikimedia.org/1/api.js")
                    .endpoint("https://hcaptcha.wikimedia.org")
                    .assethost("https://assets-hcaptcha.wikimedia.org")
                    .imghost("https://imgs-hcaptcha.wikimedia.org")
                    .reportapi("https://report-hcaptcha.wikimedia.org")
                    .sentry(false)

                    //.loading(true)
                    //.locale("en")
                    //.size(HCaptchaSize.INVISIBLE)
                    //.hideDialog(false)
                    //.tokenExpiration(10)
                    //.diagnosticLog(true)
                    //.retryPredicate { config, exception ->
                    //    exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT
                    //}

                    .build())

            hCaptcha?.addOnSuccessListener { response ->
                tokenResponse = response
                callback.onSuccess(response.tokenResult)
            }?.addOnFailureListener { e ->
                tokenResponse = null
                L.e("hCaptcha failed: ${e.message} (${e.statusCode})")
                FeedbackUtil.showMessage(activity, "${e.message} (${e.statusCode})")
            }?.addOnOpenListener {
                L.d("hCaptcha opened")
            }
        }
        hCaptcha?.verifyWithHCaptcha()
    }

    fun cleanup() {
        hCaptcha?.reset()
        hCaptcha = null
    }
}