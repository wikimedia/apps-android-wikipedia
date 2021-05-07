package org.wikipedia.feed.dayheader

import android.content.Context
import android.view.LayoutInflater
import android.widget.FrameLayout
import org.wikipedia.databinding.ViewCardDayHeaderBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.feed.view.FeedCardView

class DayHeaderCardView constructor(context: Context) : FrameLayout(context), FeedCardView<Card> {

    private val binding = ViewCardDayHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    override var callback: FeedAdapter.Callback? = null

    override var card: Card? = null
        set(value) {
            field = value
            value?.let {
                binding.dayHeaderText.text = it.title()
            }
        }
}
