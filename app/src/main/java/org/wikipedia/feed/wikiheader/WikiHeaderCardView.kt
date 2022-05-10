package org.wikipedia.feed.wikiheader

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewCardWikiHeaderBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class WikiHeaderCardView(context: Context) : FrameLayout(context), FeedCardView<Card> {

    private val binding = ViewCardWikiHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: Card? = null
    override var callback: FeedAdapter.Callback? = null
}