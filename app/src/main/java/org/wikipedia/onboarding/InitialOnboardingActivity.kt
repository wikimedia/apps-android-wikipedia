package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.settings.Prefs

class InitialOnboardingActivity : SingleFragmentActivity<InitialOnboardingFragment>(), OnboardingFragment.Callback {
    override fun onComplete() {
        setResult(RESULT_OK)
        Prefs.isInitialOnboardingEnabled = false
        finish()
    }

    override fun onBackPressed() {
        if (fragment.onBackPressed()) {
            return
        }
        setResult(RESULT_OK)
        finish()
    }

    override fun createFragment(): InitialOnboardingFragment {
        return InitialOnboardingFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, InitialOnboardingActivity::class.java)
        }
    }
}
