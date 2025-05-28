package org.wikipedia.readinglist.recommended

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class RecommendedReadingListInterestsActivity : SingleFragmentActivity<RecommendedReadingListInterestsFragment>() {
    public override fun createFragment(): RecommendedReadingListInterestsFragment {
        return RecommendedReadingListInterestsFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, RecommendedReadingListInterestsActivity::class.java)
        }
    }
}
