package org.wikipedia.suggestededits

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity


class SuggestedEditsContributionsActivity : SingleFragmentActivity<SuggestedEditsContributionsFragment>() {
    public override fun createFragment(): SuggestedEditsContributionsFragment {
        return SuggestedEditsContributionsFragment.newInstance()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        fun newIntent(ctx: Context): Intent {
            return Intent(ctx, SuggestedEditsContributionsActivity::class.java)
        }
    }
}
