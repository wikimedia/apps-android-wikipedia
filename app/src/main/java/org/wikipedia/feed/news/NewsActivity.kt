package org.wikipedia.feed.news

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.news.NewsFragment.Companion.newInstance
import org.wikipedia.json.GsonMarshaller
import org.wikipedia.json.GsonUnmarshaller
import org.wikipedia.util.ResourceUtil

class NewsActivity : SingleFragmentActivity<NewsFragment>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
    }

    public override fun createFragment(): NewsFragment {
        return newInstance(GsonUnmarshaller.unmarshal(NewsItem::class.java, intent.getStringExtra(EXTRA_NEWS_ITEM)),
            GsonUnmarshaller.unmarshal(WikiSite::class.java, intent.getStringExtra(EXTRA_WIKI)))
    }

    fun updateNavigationBarColor() {
        setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
    }

    companion object {
        const val EXTRA_NEWS_ITEM = "item"
        const val EXTRA_WIKI = "wiki"
        fun newIntent(context: Context, item: NewsItem, wiki: WikiSite): Intent {
            return Intent(context, NewsActivity::class.java)
                .putExtra(EXTRA_NEWS_ITEM, GsonMarshaller.marshal(item))
                .putExtra(EXTRA_WIKI, GsonMarshaller.marshal(wiki))
        }
    }
}
