package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.onboarding.OnboardingFragment

class ImageRecsOnboardingActivity : SingleFragmentActivity<ImageRecsOnboardingFragment>(), OnboardingFragment.Callback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.suggested_edits_image_recommendations_task_title)
    }

    override fun onComplete() {
        setResult(RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        if (fragment.onBackPressed()) {
            return
        }
        finish()
    }

    override fun createFragment(): ImageRecsOnboardingFragment {
        return ImageRecsOnboardingFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ImageRecsOnboardingActivity::class.java)
        }
    }
}
