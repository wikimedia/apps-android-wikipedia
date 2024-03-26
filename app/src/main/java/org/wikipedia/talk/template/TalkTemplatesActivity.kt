package org.wikipedia.talk.template

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle
import org.wikipedia.talk.TalkReplyActivity

class TalkTemplatesActivity : SingleFragmentActivity<TalkTemplatesFragment>() {
    public override fun createFragment(): TalkTemplatesFragment {
        return TalkTemplatesFragment.newInstance(intent.parcelableExtra(Constants.ARG_TITLE),
            intent.getLongExtra(TalkReplyActivity.FROM_REVISION_ID, -1),
            intent.getLongExtra(TalkReplyActivity.TO_REVISION_ID, -1))
    }

    companion object {
        fun newIntent(context: Context, pageTitle: PageTitle?, templateManagement: Boolean = false, fromRevisionId: Long = -1, toRevisionId: Long = -1): Intent {
            return Intent(context, TalkTemplatesActivity::class.java)
                .putExtra(Constants.ARG_TITLE, pageTitle)
                .putExtra(TalkReplyActivity.FROM_REVISION_ID, fromRevisionId)
                .putExtra(TalkReplyActivity.TO_REVISION_ID, toRevisionId)
        }
    }
}
