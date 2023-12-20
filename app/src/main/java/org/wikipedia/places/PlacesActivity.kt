package org.wikipedia.places

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.WindowManager
import org.wikipedia.activity.SingleFragmentActivity
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
        return PlacesFragment.newInstance(intent.parcelableExtra(EXTRA_TITLE), intent.parcelableExtra(EXTRA_LOCATION))
    }

    companion object {
        const val EXTRA_TITLE = "pageTitle"
        const val EXTRA_LOCATION = "location"
        fun newIntent(context: Context, pageTitle: PageTitle? = null, location: Location? = null): Intent {
            return Intent(context, PlacesActivity::class.java)
                .putExtra(EXTRA_TITLE, pageTitle)
                .putExtra(EXTRA_LOCATION, location)
        }
    }
}
