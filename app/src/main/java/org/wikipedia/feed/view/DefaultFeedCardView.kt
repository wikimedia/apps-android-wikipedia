package org.wikipedia.feed.view

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import org.wikipedia.util.L10nUtil.isLangRTL

abstract class DefaultFeedCardView<T : Card?>(context: Context?) : LinearLayout(context),
    FeedCardView<T> {
    private var card: T? = null
    private var callback: FeedAdapter.Callback? = null
    override fun setCard(card: T) {
        this.card = card
    }

    override fun getCard(): T? {
        return card
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        this.callback = callback
    }

    fun getCallback(): FeedAdapter.Callback? {
        return callback
    }

    protected fun setLayoutDirectionByWikiSite(wiki: WikiSite, rootView: View) {
        rootView.layoutDirection =
            if (isLangRTL(wiki.languageCode())) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR
    }
}
