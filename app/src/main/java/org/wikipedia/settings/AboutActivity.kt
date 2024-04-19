package org.wikipedia.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.core.view.descendants
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityAboutBinding
import org.wikipedia.richtext.setHtml
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

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

        binding.sendFeedbackTextCompose.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FilledTonalButton(
                    onClick = {
                        FeedbackUtil.composeFeedbackEmail(this@AboutActivity, "Android App ${BuildConfig.VERSION_NAME} Feedback")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(ResourceUtil.getThemedColor(this@AboutActivity, R.attr.background_color)))
                ) {
                    // TODO: needs to convert xml style to Compose Style method/class.
                    Text(
                        text = getString(R.string.send_feedback),
                        style = TextStyle(
                            fontSize = TextUnit(16f, TextUnitType.Sp),
                            fontWeight = FontWeight.Medium,
                            color = Color(ResourceUtil.getThemedColor(this@AboutActivity, R.attr.progressive_color))
                        )
                    )
                }
            }
        }
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
