package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.auth.AccountUtil

class SuggestedEditsContributionDetailsActivity : SingleFragmentActivity<SuggestedEditsContributionDetailsFragment>() {
    override fun createFragment(): SuggestedEditsContributionDetailsFragment {
        return SuggestedEditsContributionDetailsFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.title = getString(R.string.suggested_edits_contributions_screen_title, AccountUtil.getUserName())
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsContributionDetailsActivity::class.java)
        }
    }

}