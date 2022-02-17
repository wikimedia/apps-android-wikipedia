package org.wikipedia.feed.topread

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity

class TopReadArticlesActivity : SingleFragmentActivity<TopReadFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.top_read_activity_title, intent.getParcelableExtra<TopReadListCard>(TOP_READ_CARD)?.subtitle().orEmpty())
    }

    public override fun createFragment(): TopReadFragment {
        return TopReadFragment.newInstance(intent.getParcelableExtra(TOP_READ_CARD)!!)
    }

    companion object {
        const val TOP_READ_CARD = "item"
        fun newIntent(context: Context, card: TopReadListCard): Intent {
            return Intent(context, TopReadArticlesActivity::class.java)
                .putExtra(TOP_READ_CARD, card)
        }
    }
}
