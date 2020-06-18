package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.json.GsonMarshaller

class SuggestedEditsContributionDetailsActivity : SingleFragmentActivity<SuggestedEditsContributionDetailsFragment>() {
    override fun createFragment(): SuggestedEditsContributionDetailsFragment {
        return SuggestedEditsContributionDetailsFragment.newInstance()
    }

    fun updateStatusBarColor(color: Int) {
        setStatusBarColor(color)
    }

    companion object {
        const val EXTRA_SOURCE_CONTRIBUTION = "contribution"

        fun newIntent(context: Context, contribution: Contribution): Intent {
            return Intent(context, SuggestedEditsContributionDetailsActivity::class.java)
                    .putExtra(EXTRA_SOURCE_CONTRIBUTION, GsonMarshaller.marshal(contribution))
        }
    }
}
