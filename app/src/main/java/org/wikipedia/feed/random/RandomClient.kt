package org.wikipedia.feed.random

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.log.L

class RandomClient : FeedClient {

    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        clientJob = CoroutineScope(Dispatchers.Main).launch(
            CoroutineExceptionHandler { _, caught ->
                L.v(caught)
                cb.error(caught)
            }
        ) {
            val list = mutableListOf<RandomCard>()
            FeedContentType.aggregatedLanguages.forEach { lang ->
                val wikiSite = WikiSite.forLanguageCode(lang)
                val randomSummary = try {
                    ServiceFactory.getRest(wikiSite).getRandomSummary()
                } catch (e: Exception) {
                    AppDatabase.instance.readingListPageDao().getRandomPage()?.let {
                        ReadingListPage.toPageSummary(it)
                    } ?: run {
                        throw e
                    }
                }
                list.add(RandomCard(randomSummary, age, wikiSite))
            }
            FeedCoordinator.postCardsToCallback(cb, list)
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
