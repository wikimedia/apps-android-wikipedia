package org.wikipedia.feed.dataclient

import android.content.Context
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.searchbar.SearchCard

abstract class DummyClient : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        getNewCard(wiki).let {
            if (it is SearchCard) {
                cb.success(listOf(it))
            } else {
                FeedCoordinator.postCardsToCallback(cb, listOf(it))
            }
        }
    }

    override fun cancel() {}
    abstract fun getNewCard(wiki: WikiSite?): Card
}
