package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.onboarding.SuggestedEditsOnboardingFragment.Companion.newInstance
import org.wikipedia.util.ResourceUtil

class SuggestedEditsOnboardingActivity : SingleFragmentActivity<SuggestedEditsOnboardingFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.main_toolbar_color))
    }

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