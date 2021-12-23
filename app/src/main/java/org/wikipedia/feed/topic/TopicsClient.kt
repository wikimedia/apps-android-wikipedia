package org.wikipedia.feed.topic

import android.content.Context
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
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
        val position = age % topics.size
        disposables.add(ServiceFactory.get(WikiSite.forLanguageCode("en")).fullTextSearch(topics.elementAt(position), 20, null, "0").subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({ response ->
            val queryPage = response.query?.pages?.get(age)
            queryPage?.let {
                val pageSummary =
                    PageSummary(it.displayTitle("en"), it.displayTitle("en"), it.description, it.description, it.thumbUrl(), "en")
                FeedCoordinator.postCardsToCallback(cb, listOf(TopicCard(pageSummary, age, wiki,topics.elementAt(position))))
            }
        }) { t ->
            L.v(t)
            cb.error(t)
        })
    }

    override fun cancel() {
        disposables.clear()
    }
}