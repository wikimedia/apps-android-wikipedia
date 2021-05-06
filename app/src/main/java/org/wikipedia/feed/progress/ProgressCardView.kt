package org.wikipedia.feed.progress

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewCardProgressBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class ProgressCardView constructor(context: Context) : FrameLayout(context), FeedCardView<Card?> {

    init {
        ViewCardProgressBinding.inflate(LayoutInflater.from(context), this, true)
    }

    override var callback: FeedAdapter.Callback? = null
    override var card: Card? = null
}
