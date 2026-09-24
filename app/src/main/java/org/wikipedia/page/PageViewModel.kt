package org.wikipedia.page

import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.history.HistoryEntry
import org.wikipedia.readinglist.database.ReadingListPage

class PageViewModel {

    var page: Page? = null
    var title: PageTitle? = null
    var curEntry: HistoryEntry? = null
    var readingListPage: ReadingListPage? = null
    var hasWatchlistExpiry = false
    var isWatched = false
    var forceNetwork = false
    var isReadMoreLoaded = false
    val isInReadingList get() = readingListPage != null
    val cacheControl get() = if (forceNetwork) OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK else OkHttpConnectionFactory.CACHE_CONTROL_NONE
    val shouldLoadAsMobileWeb get() =
        title?.run { namespace() === Namespace.SPECIAL || isMainPage } ?: run { false } ||
          page?.run { summary.ns !== Namespace.MAIN && summary.ns !== Namespace.USER &&
                  summary.ns !== Namespace.PROJECT && summary.ns !== Namespace.DRAFT || isMainPage } ?: run { false }
}
