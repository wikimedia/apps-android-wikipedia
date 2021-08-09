package org.wikipedia.feed.news

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.json.MoshiUtil
import org.wikipedia.util.ResourceUtil

class NewsActivity : SingleFragmentActivity<NewsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
    }

    public override fun createFragment(): NewsFragment {
        val newsAdapter = MoshiUtil.getDefaultMoshi().adapter(NewsItem::class.java)
        val wikiAdapter = MoshiUtil.getDefaultMoshi().adapter(WikiSite::class.java)
        return NewsFragment.newInstance(
            newsAdapter.fromJson(intent.getStringExtra(EXTRA_NEWS_ITEM) ?: "null")!!,
            wikiAdapter.fromJson(intent.getStringExtra(EXTRA_WIKI) ?: "null")!!)
    }

    fun updateNavigationBarColor() {
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
    }

    companion object {
        const val EXTRA_NEWS_ITEM = "item"
        const val EXTRA_WIKI = "wiki"
        fun newIntent(context: Context, item: NewsItem, wiki: WikiSite): Intent {
            val newsAdapter = MoshiUtil.getDefaultMoshi().adapter(NewsItem::class.java)
            val wikiAdapter = MoshiUtil.getDefaultMoshi().adapter(WikiSite::class.java)
            return Intent(context, NewsActivity::class.java)
                .putExtra(EXTRA_NEWS_ITEM, newsAdapter.toJson(item))
                .putExtra(EXTRA_WIKI, wikiAdapter.toJson(wiki))
        }
    }
}
