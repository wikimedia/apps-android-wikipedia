package org.wikipedia.userprofile

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class ContributionsActivity : SingleFragmentActivity<ContributionsFragment>() {
    public override fun createFragment(): ContributionsFragment {
        return ContributionsFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, ContributionsActivity::class.java)
        }
    }
}
