package org.wikipedia.descriptions

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.onboarding.OnboardingFragment

class DescriptionEditTutorialActivity : SingleFragmentActivity<DescriptionEditTutorialFragment>(), OnboardingFragment.Callback {
    override fun onComplete() {
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun createFragment(): DescriptionEditTutorialFragment {
        return DescriptionEditTutorialFragment.newInstance(intent.getBooleanExtra(SHOULD_SHOW_AI_ON_BOARDING, false))
    }

    companion object {
        const val SHOULD_SHOW_AI_ON_BOARDING = "showAIOnBoarding"
        fun newIntent(context: Context, showAIOnBoarding: Boolean): Intent {
            return Intent(context, DescriptionEditTutorialActivity::class.java)
                .putExtra(SHOULD_SHOW_AI_ON_BOARDING, showAIOnBoarding)
        }
    }
}
