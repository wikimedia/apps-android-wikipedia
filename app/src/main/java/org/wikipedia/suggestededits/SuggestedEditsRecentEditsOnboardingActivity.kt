package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.onboarding.OnboardingFragment

class SuggestedEditsRecentEditsOnboardingActivity : SingleFragmentActivity<SuggestedEditsRecentEditsOnboardingFragment>(), OnboardingFragment.Callback {
    override fun onComplete() {
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun createFragment(): SuggestedEditsRecentEditsOnboardingFragment {
        return SuggestedEditsRecentEditsOnboardingFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsRecentEditsOnboardingActivity::class.java)
        }
    }
}
