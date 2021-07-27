package org.wikipedia.feed.mainpage

import android.content.Context
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.feed.FeedContentType
import org.wikipedia.feed.FeedCoordinator
import org.wikipedia.feed.dataclient.FeedClient

class MainPageClient : FeedClient {

    override fun request(context: Context, wiki: WikiSite, age: Int, cb: FeedClient.Callback) {
        val cards = WikipediaApp.instance.appLanguageState.appLanguageCodes
            .filterNot { FeedContentType.MAIN_PAGE.langCodesDisabled.contains(it) }
            .map { MainPageCard(WikiSite.forLanguageCode(it)) }
        FeedCoordinator.postCardsToCallback(cb, cards)
    }

    override fun cancel() {}
}
