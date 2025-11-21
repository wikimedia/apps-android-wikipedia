package org.wikipedia.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.wikipedia.extensions.parcelable
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.util.CustomTabsUtil

class CustomTabProxyActivity : AppCompatActivity() {
    private var tabUrl: String? = null
    private var nextIntent: Intent? = null

    private var isTabLaunched = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            tabUrl = intent.getStringExtra(EXTRA_TAB_URL)
            nextIntent = intent.parcelableExtra(EXTRA_NEXT_INTENT)
        } else {
            tabUrl = savedInstanceState.getString(EXTRA_TAB_URL)
            nextIntent = savedInstanceState.parcelable(EXTRA_NEXT_INTENT)
            isTabLaunched = savedInstanceState.getBoolean(EXTRA_TAB_LAUNCHED, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_TAB_URL, tabUrl)
        outState.putBoolean(EXTRA_TAB_LAUNCHED, isTabLaunched)
    }

    override fun onResume() {
        super.onResume()

        if (!isTabLaunched && !tabUrl.isNullOrEmpty()) {
            CustomTabsUtil.openInCustomTab(this, tabUrl!!)
            isTabLaunched = true
            return
        }

        finish()

        if (nextIntent != null) {
            startActivity(nextIntent)
            nextIntent = null
        }
    }

    companion object {
        private const val EXTRA_TAB_URL = "extra_tab_url"
        private const val EXTRA_NEXT_INTENT = "extra_next_intent"
        private const val EXTRA_TAB_LAUNCHED = "extra_tab_launched"

        fun newIntent(context: Context, customTabUrl: String): Intent {
            return Intent(context, CustomTabProxyActivity::class.java)
                .putExtra(EXTRA_TAB_URL, customTabUrl)
        }

        fun newCancelingIntent(context: Context, nextIntent: Intent?): Intent {
            return Intent(context, CustomTabProxyActivity::class.java)
                .putExtra(EXTRA_NEXT_INTENT, nextIntent).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
        }
    }
}
