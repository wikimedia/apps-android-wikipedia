package org.wikipedia.feed.offline

import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import org.wikipedia.databinding.ViewCardOfflineBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class OfflineCardView(context: Context) : LinearLayout(context), FeedCardView<Card> {

    private val binding = ViewCardOfflineBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.viewCardOfflineButtonRetry.setOnClickListener { callback?.onRetryFromOffline() }
    }

    override var callback: FeedAdapter.Callback? = null

    override var card: Card? = null

    // Hide the top padding when detached so that if this View is reused further down the feed, it
    // won't have the leftover padding inappropriately.
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.viewCardOfflineTopPadding.visibility = GONE
    }

    // This view has a transparent background, so it'll need a little padding if it appears directly
    // below the search card, so that it doesn't partially overlap the dark blue background.
    fun setTopPadding() {
        binding.viewCardOfflineTopPadding.visibility = VISIBLE
    }
}
