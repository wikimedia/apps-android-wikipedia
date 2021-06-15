package org.wikipedia.feed.dataclient

import android.content.Context
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card

interface FeedClient {

    interface Callback {
        fun success(cards: List<Card>)
        fun error(caught: Throwable)
    }

    fun request(context: Context, wiki: WikiSite, age: Int, cb: Callback)
    fun cancel()
}
