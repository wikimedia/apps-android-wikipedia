package org.wikipedia.feed.wikigames

import android.content.Context
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.dataclient.FeedClient

class WikiGamesCardClient() : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        cancel()
        if (age != 0) {
            cb.success(emptyList())
            return
        }
        val availableLanguages = FeedContentType.WIKI_GAMES.langCodesSupported
            .filter { !FeedContentType.WIKI_GAMES.langCodesDisabled.contains(it) }
            .filter { langCode ->
                WikipediaApp.instance.languageState.appLanguageCodes.contains(langCode)
            }
        val cards = availableLanguages.map { langCode ->
            WikiGamesCard(WikiSite.forLanguageCode(langCode))
        }
        cb.success(cards)
    }

    override fun cancel() { }
}
