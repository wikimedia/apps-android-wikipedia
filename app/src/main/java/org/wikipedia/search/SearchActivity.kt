package org.wikipedia.search

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.IntentFunnel

class SearchActivity : SingleFragmentActivity<SearchFragment>() {
    public override fun createFragment(): SearchFragment {
        return SearchFragment.newInstance(intent.getSerializableExtra(
                Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource?,
                intent.getStringExtra(QUERY_EXTRA))
    }

    companion object {
        const val QUERY_EXTRA = "query"

        @JvmStatic
        fun newIntent(context: Context, source: InvokeSource, query: String?): Intent {
            if (source == InvokeSource.WIDGET) {
                IntentFunnel(WikipediaApp.getInstance()).logSearchWidgetTap()
            }
            return Intent(context, SearchActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, source)
                    .putExtra(QUERY_EXTRA, query)
        }
    }
}