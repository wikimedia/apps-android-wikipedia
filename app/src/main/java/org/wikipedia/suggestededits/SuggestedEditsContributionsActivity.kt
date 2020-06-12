package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.auth.AccountUtil

class SuggestedEditsContributionsActivity : SingleFragmentActivity<SuggestedEditsContributionsFragment>() {
    public override fun createFragment(): SuggestedEditsContributionsFragment {
        return SuggestedEditsContributionsFragment.newInstance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SuggestedEditsFunnel.get().contributionsOpened()
        supportActionBar?.title = getString(R.string.suggested_edits_contributions_screen_title, AccountUtil.getUserName())
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsContributionsActivity::class.java)
        }
    }
}
