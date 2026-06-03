package org.wikipedia.feed.topread

import android.content.Context
import android.content.Intent
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.util.DateUtil

class TopReadArticlesActivity : SingleFragmentActivity<TopReadFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val card = intent.parcelableExtra<TopReadCard>(TOP_READ_CARD)
        val dateStr = card?.let { DateUtil.getShortDateString(it.articles.localDate) }
        title = getString(R.string.top_read_activity_title, dateStr.orEmpty())
    }

    public override fun createFragment(): TopReadFragment {
        return TopReadFragment.newInstance(intent.parcelableExtra(TOP_READ_CARD)!!)
    }

    companion object {
        const val TOP_READ_CARD = "item"
        fun newIntent(context: Context, card: TopReadCard): Intent {
            return Intent(context, TopReadArticlesActivity::class.java)
                .putExtra(TOP_READ_CARD, card)
        }
    }
}
