package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import android.os.Bundle

import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class SuggestedEditsContributionsActivity : SingleFragmentActivity<SuggestedEditsContributionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = 0f
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        SuggestedEditsFunnel.get().contributionsOpened()
    }

    override fun createFragment(): SuggestedEditsContributionsFragment {
        return SuggestedEditsContributionsFragment.newInstance()
    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsContributionsActivity::class.java)
        }
    }
}
