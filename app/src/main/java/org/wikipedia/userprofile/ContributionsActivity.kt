package org.wikipedia.userprofile

import android.content.Context
import android.content.Intent
import androidx.core.os.bundleOf
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class ContributionsActivity : SingleFragmentActivity<ContributionsFragment>() {

    public override fun createFragment(): ContributionsFragment {
        return ContributionsFragment.newInstance(intent.getIntExtra(EXTRA_SOURCE_CONTRIBUTIONS, 0),
                intent.getLongExtra(EXTRA_SOURCE_PAGEVIEWS, 0))
    }

    companion object {
        const val EXTRA_SOURCE_CONTRIBUTIONS = "contributions"
        const val EXTRA_SOURCE_PAGEVIEWS = "pageViews"

        fun newIntent(context: Context, contributions: Int, pageViews: Long): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, ContributionsActivity::class.java)
                .putExtras(bundleOf(EXTRA_SOURCE_CONTRIBUTIONS to contributions,
                    EXTRA_SOURCE_PAGEVIEWS to pageViews))
        }
    }
}
