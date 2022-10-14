package org.wikipedia.settings

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class DeveloperSettingsActivity : SingleFragmentActivity<DeveloperSettingsFragment>() {
    public override fun createFragment(): DeveloperSettingsFragment {
        return DeveloperSettingsFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, DeveloperSettingsActivity::class.java)
        }
    }
}
