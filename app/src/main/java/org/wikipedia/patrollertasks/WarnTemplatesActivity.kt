package org.wikipedia.patrollertasks

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class WarnTemplatesActivity : SingleFragmentActivity<WarnTemplatesFragment>() {
    public override fun createFragment(): WarnTemplatesFragment {
        return WarnTemplatesFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, WarnTemplatesActivity::class.java)
        }
    }
}
