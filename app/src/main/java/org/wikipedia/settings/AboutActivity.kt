package org.wikipedia.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.descendants
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaError
import com.hcaptcha.sdk.HCaptchaSize
import com.hcaptcha.sdk.HCaptchaTokenResponse
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityAboutBinding
import org.wikipedia.richtext.setHtml
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.log.L

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    private var hCaptcha: HCaptcha? = null
    private var tokenResponse: HCaptchaTokenResponse? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.aboutContributors.setHtml(getString(R.string.about_contributors))
        binding.aboutTranslators.setHtml(getString(R.string.about_translators_translatewiki))
        binding.aboutWmf.setHtml(getString(R.string.about_wmf))
        binding.aboutAppLicense.setHtml(getString(R.string.about_app_license))
        binding.activityAboutLibraries.setHtml(getString(R.string.libraries_list))
        binding.aboutVersionText.text = BuildConfig.VERSION_NAME
        binding.aboutLogoImage.setOnClickListener(AboutLogoClickListener())
        binding.aboutContainer.descendants.filterIsInstance<TextView>().forEach {
            it.movementMethod = LinkMovementMethodCompat.getInstance()
        }


        binding.aboutVersionText.setOnClickListener {
            verifyHCaptcha()
            //hCaptcha = HCaptcha.getClient(this).setup(getHCaptchaConfig())
            //setupHCaptchaClient(hCaptcha)
        }
    }


    private fun setupHCaptchaClient(captcha: HCaptcha?) {
        captcha?.addOnSuccessListener { response ->
            tokenResponse = response
            val userResponseToken = response.tokenResult
            L.d("hCaptcha token: $userResponseToken")
        }?.addOnFailureListener { e ->
            L.e("hCaptcha failed: ${e.message} (${e.statusCode})")
            tokenResponse = null
        }?.addOnOpenListener {
            FeedbackUtil.showMessage(this, "hCaptcha shown")
        }
    }

    private fun getHCaptchaConfig(): HCaptchaConfig {
        val size = HCaptchaSize.NORMAL
        return HCaptchaConfig.builder()
            .siteKey("10000000-ffff-ffff-ffff-000000000001") // << TODO: use our site key
            .size(size)
            .loading(true)
            .hideDialog(false)
            .tokenExpiration(10)
            .diagnosticLog(true)
            .retryPredicate { config, exception ->
                exception.hCaptchaError == HCaptchaError.SESSION_TIMEOUT
            }
            .build()
    }

    private fun verifyHCaptcha() {
        if (hCaptcha != null) {
            hCaptcha?.verifyWithHCaptcha()
        } else {
            hCaptcha = HCaptcha.getClient(this).verifyWithHCaptcha(getHCaptchaConfig())
            setupHCaptchaClient(hCaptcha)
        }
    }

    private fun resetHCaptcha() {
        hCaptcha?.reset()
        hCaptcha = null
    }

    private fun markHCaptchaUsed() {
        tokenResponse?.markUsed()
    }



    private class AboutLogoClickListener : View.OnClickListener {
        private var secretClickCount = 0
        override fun onClick(v: View) {
            ++secretClickCount
            if (secretClickCount == SECRET_CLICK_LIMIT) {
                if (Prefs.isShowDeveloperSettingsEnabled) {
                    FeedbackUtil.showMessage(v.context as Activity, R.string.show_developer_settings_already_enabled)
                } else {
                    Prefs.isShowDeveloperSettingsEnabled = true
                    FeedbackUtil.showMessage(v.context as Activity, R.string.show_developer_settings_enabled)
                }
            }
        }

        companion object {
            private const val SECRET_CLICK_LIMIT = 7
        }
    }
}
