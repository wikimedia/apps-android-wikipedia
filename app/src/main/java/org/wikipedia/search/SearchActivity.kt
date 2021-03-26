package org.wikipedia.search

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.IntentFunnel
import org.wikipedia.util.log.L
import java.lang.RuntimeException

class SearchActivity : SingleFragmentActivity<SearchFragment>() {
    public override fun createFragment(): SearchFragment {
        var source = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource?
        if (source == null) {
            if (Intent.ACTION_SEND == intent.action) {
                source = InvokeSource.INTENT_SHARE
            } else if (Intent.ACTION_PROCESS_TEXT == intent.action) {
                source = InvokeSource.INTENT_PROCESS_TEXT
            } else {
                source = InvokeSource.INTENT_OTHERS
                L.logRemoteErrorIfProd(RuntimeException("Unknown intent when launching SearchActivity: " + intent.action.orEmpty()))
            }
        }
        return SearchFragment.newInstance(source, intent.getStringExtra(QUERY_EXTRA))
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
