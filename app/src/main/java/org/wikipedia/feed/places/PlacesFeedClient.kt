package org.wikipedia.feed.places

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.FeedClient

class PlacesFeedClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private lateinit var cb: FeedClient.Callback
    private var age: Int = 0
    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        this.age = age
        this.cb = cb

        // TODO

        cb.success(listOf(PlacesCard(wiki, age)))
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
