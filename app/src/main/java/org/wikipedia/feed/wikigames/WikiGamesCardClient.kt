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
        val updatedSupportedLangCodes = (if (FeedContentType.WIKI_GAMES.langCodesSupported.isNotEmpty()) FeedContentType.WIKI_GAMES.langCodesSupported else OnThisDayGameViewModel.LANG_CODES_SUPPORTED)
            .flatMap { langCode ->
                WikipediaApp.instance.languageState.getLanguageVariants(langCode) ?: listOf(langCode)
            }

        val availableLanguages = updatedSupportedLangCodes
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
