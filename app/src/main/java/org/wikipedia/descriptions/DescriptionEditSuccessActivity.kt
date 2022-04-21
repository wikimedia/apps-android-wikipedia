package org.wikipedia.descriptions

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivityTransparent

class DescriptionEditSuccessActivity : SingleFragmentActivityTransparent<DescriptionEditSuccessFragment>(), DescriptionEditSuccessFragment.Callback {
    override fun createFragment(): DescriptionEditSuccessFragment {
        return DescriptionEditSuccessFragment.newInstance()
    }

    override fun onDismissClick() {
        setResult(RESULT_OK_FROM_EDIT_SUCCESS, intent)
        finish()
    }

    companion object {
        const val RESULT_OK_FROM_EDIT_SUCCESS = 1

        fun newIntent(context: Context, invokeSource: InvokeSource): Intent {
            return Intent(context, DescriptionEditSuccessActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
