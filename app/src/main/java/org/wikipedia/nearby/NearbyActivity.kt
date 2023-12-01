package org.wikipedia.nearby

import android.content.Context
import android.content.Intent
import android.location.Location
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelableExtra

class NearbyActivity : SingleFragmentActivity<NearbyFragment>() {

    public override fun createFragment(): NearbyFragment {
        return NearbyFragment.newInstance(intent.parcelableExtra(EXTRA_WIKI)!!, intent.parcelableExtra(EXTRA_LOCATION))
    }

    companion object {
        const val EXTRA_WIKI = "wiki"
        const val EXTRA_LOCATION = "location"
        fun newIntent(context: Context, wiki: WikiSite, location: Location? = null): Intent {
            return Intent(context, NearbyActivity::class.java)
                .putExtra(EXTRA_WIKI, wiki)
                .putExtra(EXTRA_LOCATION, location)
        }
    }
}
