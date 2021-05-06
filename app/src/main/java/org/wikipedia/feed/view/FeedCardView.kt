package org.wikipedia.feed.view

import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.FeedAdapter

interface FeedCardView<T : Card?> {
    var card: T?
    var callback: FeedAdapter.Callback?
}
