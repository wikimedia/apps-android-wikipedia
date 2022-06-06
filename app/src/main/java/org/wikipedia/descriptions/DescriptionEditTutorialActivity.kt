package org.wikipedia.descriptions

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import org.wikipedia.Constants
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

        fun newIntent(context: Context, selectedText: String?, invokeSource: Constants.InvokeSource): Intent {
            return Intent(context, DescriptionEditTutorialActivity::class.java)
                .putExtras(bundleOf(DESCRIPTION_SELECTED_TEXT to selectedText,
                    Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource))
        }
    }
}
