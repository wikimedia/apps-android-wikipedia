package org.wikipedia.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.forEach
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityAboutBinding
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.FeedbackUtil.showMessage
import org.wikipedia.util.StringUtil.fromHtml
import org.wikipedia.util.log.L

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.aboutContributors.text = fromHtml(getString(R.string.about_contributors))
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutContributors)
        binding.aboutTranslators.text = fromHtml(getString(R.string.about_translators_translatewiki))
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutTranslators)
        binding.aboutWmf.text = fromHtml(getString(R.string.about_wmf))
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutWmf)
        binding.aboutAppLicense.text = fromHtml(getString(R.string.about_app_license))
        RichTextUtil.removeUnderlinesFromLinks(binding.aboutAppLicense)
        binding.aboutVersionText.text = BuildConfig.VERSION_NAME
        RichTextUtil.removeUnderlinesFromLinks(binding.activityAboutLibraries)
        binding.aboutLogoImage.setOnClickListener(AboutLogoClickListener())
        makeEverythingClickable(binding.aboutContainer)

        binding.sendFeedbackText.setOnClickListener {
            val intent = Intent()
                    .setAction(Intent.ACTION_SENDTO)
                    .setData(Uri.parse("mailto:android-support@wikimedia.org?subject=Android App ${BuildConfig.VERSION_NAME} Feedback"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    private fun makeEverythingClickable(vg: ViewGroup) {
        vg.forEach {
            if (it is ViewGroup) {
                makeEverythingClickable(it)
            } else if (it is TextView) {
                it.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private class AboutLogoClickListener : View.OnClickListener {
        private var mSecretClickCount = 0
        override fun onClick(v: View) {
            ++mSecretClickCount
            if (isSecretClickLimitMet) {
                if (Prefs.isShowDeveloperSettingsEnabled) {
                    showMessage(v.context as Activity, R.string.show_developer_settings_already_enabled)
                } else {
                    Prefs.isShowDeveloperSettingsEnabled = true
                    showMessage(v.context as Activity, R.string.show_developer_settings_enabled)
                }
            }
        }

        private val isSecretClickLimitMet: Boolean
            get() = mSecretClickCount == SECRET_CLICK_LIMIT

        companion object {
            private const val SECRET_CLICK_LIMIT = 7
        }
    }
}
