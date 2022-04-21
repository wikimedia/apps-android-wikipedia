package org.wikipedia.feed.configure

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.feed.configure.ConfigureFragment.Companion.newInstance

class ConfigureActivity : SingleFragmentActivity<ConfigureFragment>() {
    override fun createFragment(): ConfigureFragment {
        return newInstance()
    }

    companion object {
        fun newIntent(context: Context, invokeSource: Int): Intent {
            return Intent(context, ConfigureActivity::class.java)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
