package org.wikipedia.feed.news

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.news.NewsFragment.Companion.newInstance
import org.wikipedia.util.ResourceUtil

class NewsActivity : SingleFragmentActivity<NewsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
    }

    public override fun createFragment(): NewsFragment {
        return newInstance(intent.getParcelableExtra(EXTRA_NEWS_ITEM)!!,
            intent.getParcelableExtra(EXTRA_WIKI)!!)
    }

    fun updateNavigationBarColor() {
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
    }

    companion object {
        const val EXTRA_NEWS_ITEM = "item"
        const val EXTRA_WIKI = "wiki"
        fun newIntent(context: Context, item: NewsItem, wiki: WikiSite): Intent {
            return Intent(context, NewsActivity::class.java)
                .putExtra(EXTRA_NEWS_ITEM, item)
                .putExtra(EXTRA_WIKI, wiki)
        }
    }
}
