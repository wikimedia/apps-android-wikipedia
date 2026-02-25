package org.wikipedia.feed.wikigames

import android.content.Context
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.games.onthisday.OnThisDayGameProvider
import org.wikipedia.games.onthisday.OnThisDayGameViewModel
import org.wikipedia.util.log.L
import java.time.LocalDate

class WikiGamesCardClient(private val coroutineScope: CoroutineScope) : FeedClient {

    private var job: Job? = null

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        job = coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
            L.e(throwable)
            cb.success(emptyList())
        }) {
            val cards = mutableListOf<WikiGamesCard>()
            val availableLanguages = FeedContentType.WIKI_GAMES.langCodesSupported.ifEmpty { OnThisDayGameViewModel.LANG_CODES_SUPPORTED }
                .filter { !FeedContentType.WIKI_GAMES.langCodesDisabled.contains(it) }
                .filter { langCode ->
                    WikipediaApp.instance.languageState.appLanguageCodes.contains(langCode)
                }
            availableLanguages.forEach { langCode ->
                try {
                    val games = mutableListOf<WikiGame>()
                    val wikiSite = WikiSite.forLanguageCode(langCode)
                    val gameState = OnThisDayGameProvider.getGameState(wikiSite, LocalDate.now())
                    games.add(WikiGame.OnThisDayGame(state = gameState))
                    if (games.isNotEmpty()) {
                        cards.add(WikiGamesCard(wikiSite, games))
                    }
                } catch (e: Exception) {
                    L.e(e)
                }
            }
            cb.success(cards)
        }
    }

    override fun cancel() {
        job?.cancel()
    }
}
