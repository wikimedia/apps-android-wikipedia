package org.wikipedia.games

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class GamesHubActivity : SingleFragmentActivity<GamesHubFragment>() {
    public override fun createFragment(): GamesHubFragment {
        return GamesHubFragment.newInstance()
    }

    override fun onUnreadNotification() {
        fragment.updateNotificationDot(true)
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, GamesHubActivity::class.java)
        }
    }
}
