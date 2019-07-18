package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.onboarding.SuggestedEditsOnboardingFragment.Companion.newInstance

class SuggestedEditsOnboardingActivity : SingleFragmentActivity<SuggestedEditsOnboardingFragment>() {

    override fun createFragment(): SuggestedEditsOnboardingFragment {
        return newInstance(intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as Constants.InvokeSource)
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context, source: Constants.InvokeSource): Intent {
            return Intent(context, SuggestedEditsOnboardingActivity::class.java).putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, source)
        }
    }
}