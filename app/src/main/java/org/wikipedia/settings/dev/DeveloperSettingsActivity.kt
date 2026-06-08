package org.wikipedia.settings.dev

import android.content.Context
import android.content.Intent
import org.wikipedia.R
import org.wikipedia.settings.BaseSettingsActivity

class DeveloperSettingsActivity : BaseSettingsActivity<DeveloperSettingsFragment>() {
    override val title = R.string.developer_settings_activity_title

    public override fun createFragment() = DeveloperSettingsFragment.newInstance()

    companion object {
        fun newIntent(context: Context) = Intent(context, DeveloperSettingsActivity::class.java)
    }
}
