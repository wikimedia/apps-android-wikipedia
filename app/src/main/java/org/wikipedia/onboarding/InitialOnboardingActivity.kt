package org.wikipedia.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.OnBackPressedCallback
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.settings.Prefs

class InitialOnboardingActivity : SingleFragmentActivity<InitialOnboardingFragment>(), OnboardingFragment.Callback {
    override fun onSkip() {}

    override fun onComplete() {
        setResult(if (fragment.languageChanged) RESULT_LANGUAGE_CHANGED else RESULT_OK)
        Prefs.isInitialOnboardingEnabled = false
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (fragment.onBackPressed()) {
                return
            }
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun createFragment(): InitialOnboardingFragment {
        return InitialOnboardingFragment.newInstance()
    }

    companion object {
        const val RESULT_LANGUAGE_CHANGED = 1
        fun newIntent(context: Context): Intent {
            return Intent(context, InitialOnboardingActivity::class.java)
        }
    }
}
