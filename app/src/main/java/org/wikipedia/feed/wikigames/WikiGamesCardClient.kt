package org.wikipedia.feed.wikigames

import android.content.Context
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.games.onthisday.OnThisDayGameViewModel

class WikiGamesCardClient() : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        if (age != 0) {
            cb.success(emptyList())
            return
        }
        val availableLanguages = (if (FeedContentType.WIKI_GAMES.langCodesSupported.isNotEmpty()) FeedContentType.WIKI_GAMES.langCodesSupported else OnThisDayGameViewModel.LANG_CODES_SUPPORTED)
            .filter { !FeedContentType.WIKI_GAMES.langCodesDisabled.contains(it) }
            .filter { langCode ->
                WikipediaApp.instance.languageState.appLanguageCodes.contains(langCode)
            }
        val cards = availableLanguages.map { langCode ->
            WikiGamesCard(WikiSite.forLanguageCode(langCode))
        }
        cb.success(cards)
    }

    override fun cancel() {}
}
