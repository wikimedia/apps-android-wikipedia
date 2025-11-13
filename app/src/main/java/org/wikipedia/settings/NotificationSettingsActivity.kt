package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import org.wikipedia.R

class NotificationSettingsActivity : BaseSettingsActivity<NotificationSettingsFragment>() {
    override val title = R.string.notification_preferences_title

    public override fun createFragment(): NotificationSettingsFragment {
        setResult(RESULT_OK)
        return NotificationSettingsFragment.newInstance()
    }

    companion object {
        fun newIntent(ctx: Context) = Intent(ctx, NotificationSettingsActivity::class.java)
    }
}
