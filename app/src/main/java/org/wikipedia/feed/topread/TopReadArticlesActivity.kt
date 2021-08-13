package org.wikipedia.feed.topread

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.feed.topread.TopReadFragment.Companion.newInstance

class TopReadArticlesActivity : SingleFragmentActivity<TopReadFragment>() {
    public override fun createFragment(): TopReadFragment {
        return newInstance(intent.getParcelableExtra(MOST_READ_CARD)!!)
    }

    companion object {
        const val MOST_READ_CARD = "item"
        @JvmStatic
        fun newIntent(context: Context, card: TopReadListCard): Intent {
            return Intent(context, TopReadArticlesActivity::class.java)
                .putExtra(MOST_READ_CARD, card)
        }
    }
}
