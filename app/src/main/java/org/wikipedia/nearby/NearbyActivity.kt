package org.wikipedia.nearby

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite

class NearbyActivity : SingleFragmentActivity<NearbyFragment>() {

    public override fun createFragment(): NearbyFragment {
        return NearbyFragment.newInstance(intent.getParcelableExtra(EXTRA_WIKI)!!)
    }

    companion object {
        const val EXTRA_WIKI = "wiki"
        fun newIntent(context: Context, wiki: WikiSite): Intent {
            return Intent(context, NearbyActivity::class.java)
                .putExtra(EXTRA_WIKI, wiki)
        }
    }
}
