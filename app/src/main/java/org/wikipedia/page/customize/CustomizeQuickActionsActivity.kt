package org.wikipedia.page.customize

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity

class CustomizeQuickActionsActivity : SingleFragmentActivity<CustomizeQuickActionsFragment>() {
    public override fun createFragment(): CustomizeQuickActionsFragment {
        return CustomizeQuickActionsFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, CustomizeQuickActionsActivity::class.java)
        }
    }
}
