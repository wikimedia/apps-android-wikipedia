package org.wikipedia.feed.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.extensions.setLayoutDirectionByLang
import org.wikipedia.feed.model.Card

abstract class DefaultFeedCardView<T : Card?>(context: Context?) : LinearLayout(context), FeedCardView<T> {
    override var card: T? = null
    override var callback: FeedAdapter.Callback? = null

    protected fun setLayoutDirectionByWikiSite(wiki: WikiSite, rootView: View) {
        rootView.setLayoutDirectionByLang(wiki.languageCode)
    }
}
