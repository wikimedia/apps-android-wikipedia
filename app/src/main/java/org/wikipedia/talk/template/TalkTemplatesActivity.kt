package org.wikipedia.talk.template

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class TalkTemplatesActivity : SingleFragmentActivity<TalkTemplatesFragment>() {
    public override fun createFragment(): TalkTemplatesFragment {
        return TalkTemplatesFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, TalkTemplatesActivity::class.java)
        }
    }
}
