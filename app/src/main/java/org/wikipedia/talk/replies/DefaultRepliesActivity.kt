package org.wikipedia.talk.replies

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity

class DefaultRepliesActivity : SingleFragmentActivity<DefaultRepliesFragment>() {
    override fun createFragment(): DefaultRepliesFragment {
        return DefaultRepliesFragment.newInstance(intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource)
    }

    companion object {
        fun newIntent(context: Context, invokeSource: InvokeSource): Intent {
            return Intent(context, DefaultRepliesActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
