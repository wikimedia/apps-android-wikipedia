package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.json.GsonMarshaller

class SuggestedEditsContributionDetailsActivity : SingleFragmentActivity<SuggestedEditsContributionDetailsFragment>() {
    override fun createFragment(): SuggestedEditsContributionDetailsFragment {
        return SuggestedEditsContributionDetailsFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(ContextCompat.getColor(this, R.color.green90))
    }

    companion object {
        const val EXTRA_SOURCE_CONTRIBUTION = "contribution"

        fun newIntent(context: Context, contribution: SuggestedEditsContributionsFragment.Contribution): Intent {
            return Intent(context, SuggestedEditsContributionDetailsActivity::class.java)
                    .putExtra(EXTRA_SOURCE_CONTRIBUTION, GsonMarshaller.marshal(contribution))
        }
    }
}