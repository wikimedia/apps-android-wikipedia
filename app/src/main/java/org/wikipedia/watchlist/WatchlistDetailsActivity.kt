package org.wikipedia.watchlist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import org.wikipedia.activity.SingleFragmentActivity

class WatchlistDetailsActivity : SingleFragmentActivity<WatchlistDetailsFragment>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("####", "HERE1")

    }
    override fun createFragment(): WatchlistDetailsFragment {
        return WatchlistDetailsFragment.newInstance()
    }

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, WatchlistDetailsActivity::class.java)
        }
    }
}