package org.wikipedia.readinglist.recommended

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class RecommendedReadingListOnboardingActivity : SingleFragmentActivity<RecommendedReadingListSourceFragment>() {

    public override fun createFragment(): RecommendedReadingListSourceFragment {
        return  RecommendedReadingListSourceFragment.newInstance()
    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, RecommendedReadingListOnboardingActivity::class.java)
        }
    }
}
