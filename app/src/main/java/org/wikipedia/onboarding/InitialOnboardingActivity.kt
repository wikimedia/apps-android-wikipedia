package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.addCallback
import org.wikipedia.activity.BaseActivity
import org.wikipedia.settings.Prefs

class InitialOnboardingActivity : BaseActivity(), OnboardingFragment.Callback {
    // TODO: think about the way of how to handle the navigation between onboarding screens.
    override fun onSkip() {}

    override fun onComplete() {
        Prefs.isInitialOnboardingEnabled = false
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onBackPressedDispatcher.addCallback(this) {
            setResult(RESULT_OK)
            finish()
        }
        // TODO: remove this later
        onComplete()
    }

    companion object {
        const val RESULT_LANGUAGE_CHANGED = 1
        fun newIntent(context: Context): Intent {
            return Intent(context, InitialOnboardingActivity::class.java)
        }
    }
}
