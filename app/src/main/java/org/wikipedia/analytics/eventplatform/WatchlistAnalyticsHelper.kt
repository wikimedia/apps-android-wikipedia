package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle

object WatchlistAnalyticsHelper {

    fun logWatchlistItemCountOnLoad(context: Context = WikipediaApp.instance, itemsCount: Int) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemCount:$itemsCount.displayed"
            )
        )
    }

    fun logAddedToWatchlist(title: PageTitle?, context: Context = WikipediaApp.instance) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode.orEmpty()}.added"
            )
        )
    }

    fun logRemovedFromWatchlist(title: PageTitle?, context: Context = WikipediaApp.instance) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode.orEmpty()}.removed"
            )
        )
    }

    fun logAddedToWatchlistSuccess(title: PageTitle?, context: Context = WikipediaApp.instance) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode.orEmpty()}.addSuccess"
            )
        )
    }

    fun logRemovedFromWatchlistSuccess(title: PageTitle?, context: Context = WikipediaApp.instance) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode.orEmpty()}.removeSuccess"
            )
        )
    }

    fun logFilterSelection(context: Context, excludedWikiCodes: Set<String>, includedTypeCodes: Set<String>) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "WatchlistFilterSelection.appWikis:${WikipediaApp.instance.languageState.appLanguageCodes}.excludedWikiCodes:$excludedWikiCodes.includedTypeCodes:$includedTypeCodes"
            )
        )
    }
}
