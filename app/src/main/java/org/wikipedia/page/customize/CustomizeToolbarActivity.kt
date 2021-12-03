package org.wikipedia.page.customize

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class CustomizeToolbarActivity : SingleFragmentActivity<CustomizeToolbarFragment>() {
    public override fun createFragment(): CustomizeToolbarFragment {
        return CustomizeToolbarFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, CustomizeToolbarActivity::class.java)
        }
    }
}
