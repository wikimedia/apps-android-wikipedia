package org.wikipedia.captcha

import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaDialogFragment
import com.hcaptcha.sdk.HCaptchaTheme
import com.hcaptcha.sdk.HCaptchaTokenResponse
import org.wikipedia.WikipediaApp
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.log.L
import kotlin.String

class HCaptchaHelper(
    private val activity: FragmentActivity,
    private val callback: Callback
) {
    interface Callback {
        fun onSuccess(token: String)
        fun onError(e: Exception)
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
        siteKey = "e11698d6-51ca-4980-875c-72309c6678cc"
    )

    private val dialogCancelableRunnable = MakeHCaptchaDialogCancelable()

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
                    .loading(false)
                    .build()
            )

            hCaptcha?.addOnSuccessListener { response ->
                tokenResponse = response
                callback.onSuccess(response.tokenResult)
            }?.addOnFailureListener { e ->
                tokenResponse = null
                L.e("hCaptcha failed: ${e.message} (${e.statusCode})")
                callback.onError(e)
            }?.addOnOpenListener {
                L.d("hCaptcha opened")
            }
        }
        hCaptcha?.verifyWithHCaptcha()

        // This works around an issue in the hCaptcha library where the "loading" dialog, even when
        // it's not visible, still allows itself to be "canceled" by touching anywhere outside its
        // bounds. We work around this by explicitly finding its DialogFragment and setting it to
        // be non-cancelable, and then setting it back to cancelable after a short delay, so that
        // the user doesn't accidentally tap the screen while hCaptcha is loading.
        activity.window.decorView.post(MakeHCaptchaDialogCancelable(false))
        activity.window.decorView.postDelayed(dialogCancelableRunnable, 10000)
    }

    fun cleanup() {
        if (!activity.isDestroyed) {
            activity.window.decorView.removeCallbacks(dialogCancelableRunnable)
        }
        hCaptcha?.removeAllListeners()
        hCaptcha?.reset()
        hCaptcha = null
    }

    inner class MakeHCaptchaDialogCancelable(val cancelable: Boolean = true) : Runnable {
        override fun run() {
            if (!activity.isDestroyed) {
                activity.supportFragmentManager.fragments.forEach {
                    if (it is HCaptchaDialogFragment) {
                        it.isCancelable = cancelable
                    }
                }
            }
        }
    }
}
