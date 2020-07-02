package org.wikipedia.edits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.json.GsonMarshaller

class EditsContributionDetailsActivity : SingleFragmentActivity<EditsContributionDetailsFragment>() {
    override fun createFragment(): EditsContributionDetailsFragment {
        return EditsContributionDetailsFragment.newInstance()
    }

    fun updateStatusBarColor(color: Int) {
        setStatusBarColor(color)
    }

    companion object {
        const val EXTRA_SOURCE_CONTRIBUTION = "contribution"

        fun newIntent(context: Context, contribution: Contribution): Intent {
            return Intent(context, EditsContributionDetailsActivity::class.java)
                    .putExtra(EXTRA_SOURCE_CONTRIBUTION, GsonMarshaller.marshal(contribution))
        }
    }
}
