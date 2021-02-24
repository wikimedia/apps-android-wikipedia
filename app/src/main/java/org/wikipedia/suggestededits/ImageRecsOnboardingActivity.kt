package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.onboarding.OnboardingFragment

class ImageRecsOnboardingActivity : SingleFragmentActivity<ImageRecsOnboardingFragment>(), OnboardingFragment.Callback {
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
