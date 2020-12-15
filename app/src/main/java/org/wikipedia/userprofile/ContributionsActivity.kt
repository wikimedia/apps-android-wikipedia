package org.wikipedia.userprofile

import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.SuggestedEditsFunnel

class ContributionsActivity : SingleFragmentActivity<ContributionsFragment>() {
    public override fun createFragment(): ContributionsFragment {
        return ContributionsFragment.newInstance()
    }

    companion object {
        const val EXTRA_USERNAME = "username"

        fun newIntent(context: Context, @NonNull username: String): Intent {
            SuggestedEditsFunnel.get().contributionsOpened()
            return Intent(context, ContributionsActivity::class.java)
                    .putExtra(EXTRA_USERNAME, username)
        }
    }
}
