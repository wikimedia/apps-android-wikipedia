package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.WikipediaApp

object WatchlistAnalyticsHelper {
    fun logAddedToWatchlist(context: Context, itemWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:$itemWiki.added"
            )
        )
    }

    fun logRemovedFromWatchlist(context: Context, itemWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:$itemWiki.removed"
            )
        )
    }

    fun logAddedToWatchlistSuccess(context: Context, itemWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:$itemWiki.addSuccess"
            )
        )
    }

    fun logRemovedFromWatchlistSuccess(context: Context, itemWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:$itemWiki.removeSuccess"
            )
        )
    }

    fun logFilterSelection(context: Context, excludedWikiCodes: MutableSet<String>, includedTypeCodes: MutableSet<String>) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "WatchlistFilterSelection.appWikis:${WikipediaApp.instance.languageState.appLanguageCodes}.excludedWikiCodes:$excludedWikiCodes.includedTypeCodes:$includedTypeCodes"
            )
        )
    }
}
