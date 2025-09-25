package org.wikipedia.captcha

import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaTheme
import com.hcaptcha.sdk.HCaptchaTokenResponse
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L
import kotlin.String

class HCaptchaHelper(
    private val activity: FragmentActivity,
    private val callback: Callback
) {
    fun interface Callback {
        fun onSuccess(token: String)
    }

    private var hCaptcha: HCaptcha? = null
    private var tokenResponse: HCaptchaTokenResponse? = null

    private val configDefault get() = RemoteConfig.RemoteConfigHCaptcha(
        baseURL = "https://meta.wikimedia.org",
        jsSrc = "https://assets-hcaptcha.wikimedia.org/1/api.js",
        endpoint = "https://hcaptcha.wikimedia.org",
        assetHost = "https://assets-hcaptcha.wikimedia.org",
        imgHost = "https://imgs-hcaptcha.wikimedia.org",
        reportApi = "https://report-hcaptcha.wikimedia.org",
        sentry = false,
        siteKey = "f1f21d64-6384-4114-b7d0-d9d23e203b4a"
    )

    fun show() {
        if (hCaptcha == null) {
            val config = RemoteConfig.config.androidv1?.hCaptcha ?: configDefault
            hCaptcha = HCaptcha.getClient(activity)
            hCaptcha?.setup(
                HCaptchaConfig.builder()
                    .theme(if (WikipediaApp.instance.currentTheme.isDark) HCaptchaTheme.DARK else HCaptchaTheme.LIGHT)
                    .siteKey(config.siteKey)
                    .host(config.baseURL.toUri().host)
                    .jsSrc(config.jsSrc)
                    .endpoint(config.endpoint)
                    .assethost(config.assetHost)
                    .imghost(config.imgHost)
                    .reportapi(config.reportApi)
                    .sentry(config.sentry)
                    .loading(true)
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
