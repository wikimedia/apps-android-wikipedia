package org.wikipedia.places

import android.content.Context
import android.content.Intent
import android.location.Location
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle

class PlacesActivity : SingleFragmentActivity<PlacesFragment>() {

    public override fun createFragment(): PlacesFragment {
        return PlacesFragment.newInstance(intent.parcelableExtra(EXTRA_WIKI)!!, intent.parcelableExtra(EXTRA_TITLE), intent.parcelableExtra(EXTRA_LOCATION))
    }

    companion object {
        const val EXTRA_WIKI = "wiki"
        const val EXTRA_TITLE = "pageTitle"
        const val EXTRA_LOCATION = "location"
        fun newIntent(context: Context, wiki: WikiSite, pageTitle: PageTitle? = null, location: Location? = null): Intent {
            return Intent(context, PlacesActivity::class.java)
                .putExtra(EXTRA_WIKI, wiki)
                .putExtra(EXTRA_TITLE, pageTitle)
                .putExtra(EXTRA_LOCATION, location)
        }
    }
}
