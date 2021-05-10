package org.wikipedia.feed.view

import org.wikipedia.feed.model.Card

interface FeedCardView<T : Card?> {
    var card: T?
    var callback: FeedAdapter.Callback?
}
