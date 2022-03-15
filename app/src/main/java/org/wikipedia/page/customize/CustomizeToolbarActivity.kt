package org.wikipedia.page.customize

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class CustomizeToolbarActivity : SingleFragmentActivity<CustomizeToolbarFragment>() {
    public override fun createFragment(): CustomizeToolbarFragment {
        return CustomizeToolbarFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, CustomizeToolbarActivity::class.java)
        }
    }
}
