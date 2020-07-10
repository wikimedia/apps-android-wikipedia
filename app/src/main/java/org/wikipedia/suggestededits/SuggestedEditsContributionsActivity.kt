package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class SuggestedEditsContributionsActivity : SingleFragmentActivity<SuggestedEditsContributionsFragment>() {
    public override fun createFragment(): SuggestedEditsContributionsFragment {
        return SuggestedEditsContributionsFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, SuggestedEditsContributionsActivity::class.java)
        }
    }
}
