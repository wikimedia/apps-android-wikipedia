package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.suggestededits.SuggestedEditsContributionsFragment.Contribution


class SuggestedEditsContributionsActivity : SingleFragmentActivity<SuggestedEditsContributionsFragment>() {
    public override fun createFragment(): SuggestedEditsContributionsFragment {
        return SuggestedEditsContributionsFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.title = getString(R.string.suggested_edits_contributions_screen_title, AccountUtil.getUserName())
    }

    companion object {
        const val ARG_CONTRIBUTIONS_LIST = "contributions"
        const val ARG_CONTRIBUTIONS_CONTINUE = "continue"
        fun newIntent(context: Context, contributions: List<Contribution>, userContributionsContinuation: String): Intent {
            return Intent(context, SuggestedEditsContributionsActivity::class.java).putExtra(ARG_CONTRIBUTIONS_LIST, GsonMarshaller.marshal(contributions)).putExtra(ARG_CONTRIBUTIONS_CONTINUE, userContributionsContinuation)
        }
    }
}
