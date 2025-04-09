package org.wikipedia.feed.topic

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.settings.Prefs
import org.wikipedia.util.log.L

class TopicsClient(
    private val coroutineScope: CoroutineScope
) : FeedClient {

    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        clientJob = coroutineScope.launch(
            CoroutineExceptionHandler { _, caught ->
                L.v(caught)
                cb.error(caught)
            }
        ) {
            val cardsList = mutableListOf<TopicCard>()
            val topics = Prefs.selectedTopics.toMutableSet()
            if (topics.isNotEmpty()) {
                val position = age % topics.size

                val response = ServiceFactory.get(WikiSite.forLanguageCode("en"))
                    .fullTextSearch("articletopic:" + topics.elementAt(position), 20, null)
                response.query?.pages?.get(age)?.let {
                    val pageSummary = PageSummary(
                        it.displayTitle("en"), it.displayTitle("en"),
                        it.description, it.description, it.thumbUrl(), "en"
                    )
                    cardsList.add(TopicCard(pageSummary, age, wiki, topics.elementAt(position)))
                }

                cb.success(cardsList)
            }
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
