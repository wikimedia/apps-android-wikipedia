package org.wikipedia.talk.template

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle

class TalkTemplatesActivity : SingleFragmentActivity<TalkTemplatesFragment>() {
    public override fun createFragment(): TalkTemplatesFragment {
        val title = intent.parcelableExtra<PageTitle>(Constants.ARG_TITLE)!!
        return TalkTemplatesFragment.newInstance(title)
    }

    companion object {
        fun newIntent(context: Context, pageTitle: PageTitle): Intent {
            return Intent(context, TalkTemplatesActivity::class.java)
                .putExtra(Constants.ARG_TITLE, pageTitle)
        }
    }
}
