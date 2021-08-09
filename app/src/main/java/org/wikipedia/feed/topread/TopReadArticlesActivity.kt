package org.wikipedia.feed.topread

import android.content.Context
import android.content.Intent
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.json.MoshiUtil

class TopReadArticlesActivity : SingleFragmentActivity<TopReadFragment>() {
    public override fun createFragment(): TopReadFragment {
        val adapter = MoshiUtil.getDefaultMoshi().adapter(TopReadItemCard::class.java)
        return TopReadFragment.newInstance(adapter.fromJson(intent.getStringExtra(MOST_READ_CARD) ?: "null")!!)
    }

    companion object {
        const val MOST_READ_CARD = "item"
        @JvmStatic
        fun newIntent(context: Context, card: TopReadListCard): Intent {
            val adapter = MoshiUtil.getDefaultMoshi().adapter(TopReadListCard::class.java)
            return Intent(context, TopReadArticlesActivity::class.java)
                .putExtra(MOST_READ_CARD, adapter.toJson(card))
        }
    }
}
