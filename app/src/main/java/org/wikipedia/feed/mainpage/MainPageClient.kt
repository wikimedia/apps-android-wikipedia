package org.wikipedia.feed.mainpage

import android.content.Context
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient
import org.wikipedia.feed.model.Card
import java.util.*

class MainPageClient : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        val cards = mutableListOf<Card>()
        for (appLangCode in WikipediaApp.getInstance().language().appLanguageCodes) {
            if (!FeedContentType.MAIN_PAGE.langCodesDisabled.contains(appLangCode)) {
                cards.add(MainPageCard(WikiSite.forLanguageCode(appLangCode)))
            }
        }
        FeedCoordinator.postCardsToCallback(cb, cards)
    }

    override fun cancel() {}
}
