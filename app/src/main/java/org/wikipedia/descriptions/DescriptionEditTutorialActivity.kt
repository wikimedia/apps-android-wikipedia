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
        return DescriptionEditTutorialFragment.newInstance()
    }

    companion object {
        const val DESCRIPTION_SELECTED_TEXT = "selectedText"

        @JvmStatic
        fun newIntent(context: Context, selectedText: String?): Intent {
            return Intent(context, DescriptionEditTutorialActivity::class.java)
                    .putExtra(DESCRIPTION_SELECTED_TEXT, selectedText)
        }
    }
}
