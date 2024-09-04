package org.wikipedia.feed.random

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.log.L

class RandomClient(
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
            val list = mutableListOf<RandomCard>()
            WikipediaApp.instance.languageState.appLanguageCodes.filter { !FeedContentType.RANDOM.langCodesDisabled.contains(it) }.forEach { lang ->
                val wikiSite = WikiSite.forLanguageCode(lang)
                val randomSummary = try {
                    ServiceFactory.getRest(wikiSite).getRandomSummary()
                } catch (e: Exception) {
                    AppDatabase.instance.readingListPageDao().getRandomPage(lang)?.let {
                        ReadingListPage.toPageSummary(it)
                    }
                }
                randomSummary?.let {
                    list.add(RandomCard(it, age, wikiSite))
                }
            }
            cb.success(list)
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
