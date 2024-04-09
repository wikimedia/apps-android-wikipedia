package org.wikipedia.feed.topic

import android.content.Context
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class TopicsClient : FeedClient {

    private val disposables = CompositeDisposable()

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        val topics = Prefs.selectedTopics.toMutableSet()
        if (topics.isEmpty()) {
            return
        }
        val position = age % topics.size

        CoroutineScope(Dispatchers.Default).launch(CoroutineExceptionHandler { _, caught ->
            L.v(caught)
            cb.error(caught)
        }) {
            val response = ServiceFactory.get(WikiSite.forLanguageCode("en"))
                .fullTextSearch("articletopic:" + topics.elementAt(position), 20, null)
            response.query?.pages?.get(age)?.let {
                val pageSummary = PageSummary(it.displayTitle("en"), it.displayTitle("en"),
                    it.description, it.description, it.thumbUrl(), "en")
                FeedCoordinator.postCardsToCallback(cb, listOf(TopicCard(pageSummary, age, wiki, topics.elementAt(position))))
            }
        }
    }

    override fun cancel() {
        disposables.clear()
    }
}
