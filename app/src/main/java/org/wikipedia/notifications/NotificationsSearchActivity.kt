package org.wikipedia.notifications

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.databinding.ActivityNotificationsSearchBinding
import org.wikipedia.util.ResourceUtil

class NotificationsSearchActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationsSearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleIntent(intent)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.nav_tab_background_color))
        setSupportActionBar(binding.searchToolbar)
        supportActionBar?.title = ""
        binding.searchCabView.isIconified = false

        binding.searchCabView.disableCloseButton()
    }

    private fun handleIntent(intent: Intent) {

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            // use the query to search your data somehow
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, NotificationsSearchActivity::class.java)
        }
    }
}
