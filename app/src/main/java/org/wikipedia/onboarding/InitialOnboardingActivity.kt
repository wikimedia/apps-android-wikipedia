package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.views.ActivityGestureListener

class InitialOnboardingActivity : SingleFragmentActivity<InitialOnboardingFragment>(), OnboardingFragment.Callback {

    private var gestureDetector: GestureDetector? = null

    override fun onComplete() {
        setResult(RESULT_OK)
        Prefs.isInitialOnboardingEnabled = false
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gestureDetector = GestureDetector(this, ActivityGestureListener(this))
    }

    override fun onBackPressed() {
        if (fragment.onBackPressed()) {
            return
        }
        setResult(RESULT_OK)
        finish()
    }

    override fun getGestureDetectorForBreadCrumbs(): GestureDetector? {
        return gestureDetector
    }

    override fun createFragment(): InitialOnboardingFragment {
        return InitialOnboardingFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, InitialOnboardingActivity::class.java)
        }
    }
}
