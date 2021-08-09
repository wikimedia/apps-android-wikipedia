package org.wikipedia.userprofile

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.json.MoshiUtil

class ContributionDetailsActivity : SingleFragmentActivity<ContributionDetailsFragment>() {
    override fun createFragment(): ContributionDetailsFragment {
        return ContributionDetailsFragment.newInstance()
    }

    fun updateStatusBarColor(color: Int) {
        setStatusBarColor(color)
    }

    companion object {
        const val EXTRA_SOURCE_CONTRIBUTION = "contribution"

        fun newIntent(context: Context, contribution: Contribution): Intent {
            val adapter = MoshiUtil.getDefaultMoshi().adapter(Contribution::class.java)
            return Intent(context, ContributionDetailsActivity::class.java)
                    .putExtra(EXTRA_SOURCE_CONTRIBUTION, adapter.toJson(contribution))
        }
    }
}
