package org.wikipedia.places

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite

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
