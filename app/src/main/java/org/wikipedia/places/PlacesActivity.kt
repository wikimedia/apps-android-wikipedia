package org.wikipedia.places

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import com.mapbox.mapboxsdk.geometry.LatLng
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle

class PlacesActivity : SingleFragmentActivity<PlacesFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    public override fun createFragment(): PlacesFragment {
        return PlacesFragment.newInstance(intent.parcelableExtra(EXTRA_WIKI)!!, intent.parcelableExtra(EXTRA_TITLE), intent.parcelableExtra(EXTRA_LOCATION))
    }

    companion object {
        const val EXTRA_WIKI = "wiki"
        const val EXTRA_TITLE = "pageTitle"
        const val EXTRA_LOCATION = "location"
        fun newIntent(context: Context, wiki: WikiSite, pageTitle: PageTitle? = null, location: LatLng? = null): Intent {
            return Intent(context, PlacesActivity::class.java)
                .putExtra(EXTRA_WIKI, wiki)
                .putExtra(EXTRA_TITLE, pageTitle)
                .putExtra(EXTRA_LOCATION, location)
        }
    }
}
