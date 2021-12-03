package org.wikipedia.page.customize

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class CustomizeFavoritesActivity : SingleFragmentActivity<CustomizeFavoritesFragment>() {
    public override fun createFragment(): CustomizeFavoritesFragment {
        return CustomizeFavoritesFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, CustomizeFavoritesActivity::class.java)
        }
    }
}
