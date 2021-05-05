package org.wikipedia.feed.accessibility

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import org.wikipedia.databinding.ViewCardAccessibilityBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class AccessibilityCardView(context: Context?) : LinearLayout(context), FeedCardView<Card?> {
    private var _binding: ViewCardAccessibilityBinding? = null
    private val binding get() = _binding!!

    private var callback: FeedAdapter.Callback? = null

    init {
        _binding = ViewCardAccessibilityBinding.inflate(LayoutInflater.from(context), this, false)
        addView(binding.root)
        binding.loadMore.setOnClickListener { onLoadMoreClick() }
    }

    fun onLoadMoreClick() {
        if (callback != null) {
            callback!!.onRequestMore()
        }
    }

    override fun setCallback(callback: FeedAdapter.Callback?) {
        this.callback = callback
    }

    override fun setCard(card: Card) {}

    override fun getCard(): Card? {
        return null
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        _binding = null
    }
}
