package org.wikipedia.edits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class EditsContributionsActivity : SingleFragmentActivity<EditsContributionsFragment>() {
    public override fun createFragment(): EditsContributionsFragment {
        return EditsContributionsFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, EditsContributionsActivity::class.java)
        }
    }
}
