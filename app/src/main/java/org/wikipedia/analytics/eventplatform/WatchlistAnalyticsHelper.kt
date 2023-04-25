package org.wikipedia.analytics.eventplatform

import android.content.Context
import android.view.MenuItem

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

    fun logAddedToWatchlistFailure(context: Context, itemWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:$itemWiki.added"
            )
        )
    }

    fun logRemovedToWatchlistFailure(context: Context, itemWiki: String) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "Watchlist.itemWiki:$itemWiki.removed"
            )
        )
    }

    fun logMenuItemClicked(context: Context, item: MenuItem) {
        BreadCrumbLogEvent.logClick(context, item)
    }
}
