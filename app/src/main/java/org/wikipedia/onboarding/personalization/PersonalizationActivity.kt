package org.wikipedia.onboarding.personalization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme

class PersonalizationActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                PersonalizationScreen(
                    onSkipClick = { finish() }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, PersonalizationActivity::class.java)
        }
    }
}
