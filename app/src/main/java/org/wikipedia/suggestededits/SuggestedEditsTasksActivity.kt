package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class SuggestedEditsTasksActivity : SingleFragmentActivity<SuggestedEditsTasksFragment>() {

    override fun onUnreadNotification() {
        fragment.updateNotificationDot(true)
    }

    override fun createFragment(): SuggestedEditsTasksFragment {
        return SuggestedEditsTasksFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsTasksActivity::class.java)
        }
    }
}
