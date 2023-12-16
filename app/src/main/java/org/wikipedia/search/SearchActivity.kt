package org.wikipedia.search

import android.content.Context
import android.content.Intent
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.util.log.L

class SearchActivity : SingleFragmentActivity<SearchFragment>() {
    public override fun createFragment(): SearchFragment {
        var source = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource?
        if (source == null) {
            when {
                Intent.ACTION_SEND == intent.action -> { source = InvokeSource.INTENT_SHARE }
                Intent.ACTION_PROCESS_TEXT == intent.action -> { source = InvokeSource.INTENT_PROCESS_TEXT }
                else -> {
                    source = InvokeSource.INTENT_UNKNOWN
                    L.logRemoteErrorIfProd(RuntimeException("Unknown intent when launching SearchActivity: " + intent.action.orEmpty()))
                }
            }
        }
        return SearchFragment.newInstance(source, intent.getStringExtra(QUERY_EXTRA), intent.getBooleanExtra(EXTRA_RETURN_LINK, false))
    }

    companion object {
        const val QUERY_EXTRA = "query"
        const val EXTRA_RETURN_LINK = "returnLink"
        const val EXTRA_RETURN_LINK_TITLE = "returnLinkTitle"
        const val RESULT_LINK_SUCCESS = 1

        fun newIntent(context: Context, source: InvokeSource, query: String?, returnLink: Boolean = false): Intent {
            return Intent(context, SearchActivity::class.java)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, source)
                    .putExtra(QUERY_EXTRA, query)
                    .putExtra(EXTRA_RETURN_LINK, returnLink)
        }
    }
}
