package org.wikipedia.feed.dataclient

import android.content.Context
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.model.Card

abstract class DummyClient : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        getNewCard(wiki).let {
            cb.success(listOf(it))
        }
    }

    override fun cancel() {}
    abstract fun getNewCard(wiki: WikiSite?): Card
}
