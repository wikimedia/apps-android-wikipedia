package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.WikipediaApp
import org.wikipedia.page.PageTitle

object WatchlistAnalyticsHelper {
    fun logAddedToWatchlist(context: Context, title: PageTitle?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode ?: ""}.added"
            )
        )
    }

    fun logRemovedFromWatchlist(context: Context, title: PageTitle?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode ?: ""}.removed"
            )
        )
    }

    fun logAddedToWatchlistSuccess(context: Context, title: PageTitle?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode ?: ""}.addSuccess"
            )
        )
    }

    fun logRemovedFromWatchlistSuccess(context: Context, title: PageTitle?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:${title?.wikiSite?.languageCode ?: ""}.removeSuccess"
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
