package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        return newInstance()
    }

    companion object {
        @JvmStatic
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsOnboardingActivity::class.java)
        }
    }
}