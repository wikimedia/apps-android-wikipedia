package org.wikipedia.readinglist.recommended

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import org.wikipedia.activity.SingleFragmentActivity

class RecommendedReadingListOnboardingActivity : SingleFragmentActivity<Fragment>() {

    public override fun createFragment(): Fragment {
        val fromSetting = intent.getBooleanExtra(EXTRA_FROM_SETTING, false)
        val startFromSourceSelection = intent.getBooleanExtra(EXTRA_START_FROM_SOURCE_SELECTION, true)
        return if (startFromSourceSelection) {
            RecommendedReadingListSourceFragment.newInstance(fromSetting)
        } else {
            // TODO: add this for the article interests screen when it is ready
            Fragment()
        }
    }

    companion object {

        private const val EXTRA_START_FROM_SOURCE_SELECTION = "startFromSourceSelection"
        private const val EXTRA_FROM_SETTING = "fromSetting"

        fun newIntent(context: Context, startFromSourceSelection: Boolean = true, fromSetting: Boolean = false): Intent {
            return Intent(context, RecommendedReadingListOnboardingActivity::class.java).apply {
                putExtra(EXTRA_START_FROM_SOURCE_SELECTION, startFromSourceSelection)
                putExtra(EXTRA_FROM_SETTING, fromSetting)
            }
        }
    }
}
