package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.OnBackPressedCallback
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.onboarding.OnboardingFragment

class SuggestedEditsRecentEditsOnboardingActivity : SingleFragmentActivity<SuggestedEditsRecentEditsOnboardingFragment>(), OnboardingFragment.Callback {
    override fun onSkip() {
        PatrollerExperienceEvent.logAction("onboarding_skip", "pt_onboarding")
    }

    override fun onComplete() {
        PatrollerExperienceEvent.logAction("get_started", "pt_onboarding")
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                PatrollerExperienceEvent.logAction("back", "pt_onboarding")
                finish()
            }
        })
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
