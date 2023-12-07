package org.wikipedia.places

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite

class PlacesActivity : SingleFragmentActivity<PlacesFragment>() {

    public override fun createFragment(): PlacesFragment {
        return PlacesFragment.newInstance(intent.getParcelableExtra(EXTRA_WIKI)!!)
    }

    companion object {
        const val EXTRA_WIKI = "wiki"
        fun newIntent(context: Context, wiki: WikiSite): Intent {
            return Intent(context, PlacesActivity::class.java)
                .putExtra(EXTRA_WIKI, wiki)
        }
    }
}
