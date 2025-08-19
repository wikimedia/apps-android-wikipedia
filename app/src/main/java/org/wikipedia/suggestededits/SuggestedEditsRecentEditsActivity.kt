package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.settings.Prefs

class SuggestedEditsRecentEditsActivity : SingleFragmentActivity<SuggestedEditsRecentEditsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        maybeShowOnboarding()
        PatrollerExperienceEvent.logImpression("pt_recent_changes")

        onBackPressedDispatcher.addCallback(this) {
            PatrollerExperienceEvent.logAction("back", "pt_recent_changes")
            finish()
        }
    }

    public override fun createFragment(): SuggestedEditsRecentEditsFragment {
        return SuggestedEditsRecentEditsFragment.newInstance()
    }

    override fun onUnreadNotification() {
        fragment.updateNotificationDot(true)
    }

    private fun maybeShowOnboarding() {
        if (!Prefs.recentEditsOnboardingShown) {
            Prefs.recentEditsOnboardingShown = true
            startActivity(SuggestedEditsRecentEditsOnboardingActivity.newIntent(this))
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsRecentEditsActivity::class.java)
        }
    }
}
