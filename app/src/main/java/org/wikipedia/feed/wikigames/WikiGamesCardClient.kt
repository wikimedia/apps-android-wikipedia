package org.wikipedia.feed.wikigames

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.util.log.L

class WikiGamesCardClient(private val coroutineScope: CoroutineScope) : FeedClient {
    private var clientJob: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        if (age != 0) {
            cb.success(emptyList())
            return
        }

        clientJob = coroutineScope.launch(
            CoroutineExceptionHandler { _, caught ->
                L.v(caught)
                cb.error(caught)
            }) {

            val feedAvailability = ServiceFactory.getRest(wiki).feedAvailability()
            val availableLanguages = feedAvailability.games.filter { langCode ->
                WikipediaApp.instance.languageState.appLanguageCodes.contains(langCode)
            }
            val cards = availableLanguages.map { langCode ->
                WikiGamesCard(WikiSite.forLanguageCode(langCode))
            }
            cb.success(cards)
        }
    }

    override fun cancel() {
        clientJob?.cancel()
    }
}
