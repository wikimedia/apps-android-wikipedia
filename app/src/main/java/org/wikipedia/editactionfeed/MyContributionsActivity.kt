package org.wikipedia.editactionfeed

import android.content.Context
import android.content.Intent
import android.os.Bundle

import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class MyContributionsActivity : SingleFragmentActivity<MyContributionsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar!!.elevation = 0f
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        SuggestedEditsFunnel.get().contributionsOpened()
    }

    override fun createFragment(): MyContributionsFragment {
        return MyContributionsFragment.newInstance()
    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, MyContributionsActivity::class.java)
        }
    }
}
