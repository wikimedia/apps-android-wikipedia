package org.wikipedia.feed.view

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card
import java.util.*

abstract class DefaultFeedCardView<T : Card?>(context: Context?) : LinearLayout(context), FeedCardView<T> {
    override var card: T? = null
    override var callback: FeedAdapter.Callback? = null

    protected fun setLayoutDirectionByWikiSite(wiki: WikiSite, rootView: View) {
        rootView.layoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale(wiki.languageCode()))
    }
}
