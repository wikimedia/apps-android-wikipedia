package org.wikipedia.suggestededits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.onboarding.OnboardingPageView

class SuggestedEditsImageRecsOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggested_edits_image_recs_onboarding)
        val onboardingView = findViewById<OnboardingPageView>(R.id.onboarding_view)
        onboardingView.setSecondaryText(getString(R.string.image_recommendation_onboarding_2) +
                "\n\n" + getString(R.string.image_recommendation_onboarding_3))

        findViewById<View>(R.id.onboarding_done_button).setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsImageRecsOnboardingActivity::class.java)
        }
    }
}
