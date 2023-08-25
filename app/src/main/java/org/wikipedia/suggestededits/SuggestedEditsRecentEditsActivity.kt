package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class SuggestedEditsRecentEditsActivity : SingleFragmentActivity<SuggestedEditsRecentEditsFragment>() {
    public override fun createFragment(): SuggestedEditsRecentEditsFragment {
        return SuggestedEditsRecentEditsFragment.newInstance()
    }

    override fun onUnreadNotification() {
        fragment.updateNotificationDot(true)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsRecentEditsActivity::class.java)
        }
    }
}
