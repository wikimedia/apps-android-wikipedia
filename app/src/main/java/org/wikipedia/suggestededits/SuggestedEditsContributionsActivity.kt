package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.ContributionObject


class SuggestedEditsContributionsActivity : SingleFragmentActivity<SuggestedEditsContributionsFragment>() {
    public override fun createFragment(): SuggestedEditsContributionsFragment {
        return SuggestedEditsContributionsFragment.newInstance()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val ARG_CONTRIBUTIONS_LIST = "contributions"
        fun newIntent(context: Context, contributionObjects: List<ContributionObject>): Intent {
            return Intent(context, SuggestedEditsContributionsActivity::class.java).putExtra(ARG_CONTRIBUTIONS_LIST, GsonMarshaller.marshal(contributionObjects))
        }
    }
}
