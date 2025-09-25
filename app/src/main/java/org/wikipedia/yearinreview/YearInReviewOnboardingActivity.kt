package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme

class YearInReviewOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaseTheme {
                YearInReviewOnboardingScreen(
                    onBackButtonClick = {
                        finish()
                    },
                    onGetStartedClick = {
                        startActivity(YearInReviewActivity.newIntent(this))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, YearInReviewOnboardingActivity::class.java)
        }
    }
}
