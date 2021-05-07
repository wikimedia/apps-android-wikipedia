package org.wikipedia.feed.accessibility

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.wikipedia.databinding.ViewCardAccessibilityBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class AccessibilityCardView(context: Context) : LinearLayout(context), FeedCardView<Card> {

    private val binding = ViewCardAccessibilityBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.loadMore.setOnClickListener { onLoadMoreClick() }
    }

    override var card: Card? = null

    override var callback: FeedAdapter.Callback? = null

    private fun onLoadMoreClick() {
        callback?.onRequestMore()
    }
}
