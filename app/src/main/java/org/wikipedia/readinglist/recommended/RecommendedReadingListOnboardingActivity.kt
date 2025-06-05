package org.wikipedia.readinglist.recommended

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import org.wikipedia.activity.SingleFragmentActivity

class RecommendedReadingListOnboardingActivity : SingleFragmentActivity<Fragment>() {

    public override fun createFragment(): Fragment {
        val fromSettings = intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
        val startFromSourceSelection = intent.getBooleanExtra(EXTRA_START_FROM_SOURCE_SELECTION, true)
        return if (startFromSourceSelection) {
            RecommendedReadingListSourceFragment.newInstance(fromSettings)
        } else {
            RecommendedReadingListInterestsFragment.newInstance(fromSettings)
        }
    }

    companion object {
        private const val EXTRA_START_FROM_SOURCE_SELECTION = "startFromSourceSelection"
        const val EXTRA_FROM_SETTINGS = "fromSettings"

        fun newIntent(context: Context, startFromSourceSelection: Boolean = true, fromSettings: Boolean = false): Intent {
            return Intent(context, RecommendedReadingListOnboardingActivity::class.java).apply {
                putExtra(EXTRA_START_FROM_SOURCE_SELECTION, startFromSourceSelection)
                putExtra(EXTRA_FROM_SETTINGS, fromSettings)
            }
        }
    }
}
