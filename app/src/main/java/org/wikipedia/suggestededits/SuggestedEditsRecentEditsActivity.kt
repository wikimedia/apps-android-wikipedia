package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.settings.Prefs

class SuggestedEditsRecentEditsActivity : SingleFragmentActivity<SuggestedEditsRecentEditsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.showRecentEditsOnboarding = true
        maybeShowOnboarding()
    }

    public override fun createFragment(): SuggestedEditsRecentEditsFragment {
        return SuggestedEditsRecentEditsFragment.newInstance()
    }

    override fun onUnreadNotification() {
        fragment.updateNotificationDot(true)
    }

    private fun maybeShowOnboarding() {
        if (Prefs.showRecentEditsOnboarding) {
            Prefs.showRecentEditsOnboarding = false
            startActivity(SuggestedEditsRecentEditsOnboardingActivity.newIntent(this))
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsRecentEditsActivity::class.java)
        }
    }
}
