package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.NotificationPreferencesFunnel

class NotificationSettingsActivity : SingleFragmentActivity<NotificationSettingsFragment>() {
    public override fun createFragment(): NotificationSettingsFragment {
        setResult(RESULT_OK)
        return NotificationSettingsFragment.newInstance()
    }

    override fun onDestroy() {
        NotificationPreferencesFunnel(WikipediaApp.getInstance()).done()
        super.onDestroy()
    }

    companion object {
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, NotificationSettingsActivity::class.java)
        }
    }
}
