package org.wikipedia.page.pageload

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.history.HistoryEntry
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.util.UriUtil
import retrofit2.Response

class PageDataFetcher {

    suspend fun fetchPage(title: PageTitle, entry: HistoryEntry, forceNetwork: Boolean = false): PageResult {
        return try {
            val cacheControl = if (forceNetwork) "no-cache" else "default"

            val pageSummary = fetchPageSummary(title, cacheControl)
            val watchStatus = fetchWatchStatus(title)
            val categories = fetchCategories(title, watchStatus.myQueryResponse)

            if (pageSummary.body() == null) {
                throw RuntimeException("Summary response was invalid.")
            }

            PageResult.Success(
                pageSummaryResponse = pageSummary,
                categories = categories,
                isWatched = watchStatus.isWatched,
                hasWatchlistExpiry = watchStatus.hasWatchlistExpiry,
                redirectedFrom = if (pageSummary.raw().priorResponse?.isRedirect == true) title.displayText else null
            )
        } catch (e: Exception) {
            PageResult.Error(e)
        }
    }
    private suspend fun fetchPageSummary(title: PageTitle, cacheControl: String): Response<PageSummary> {
        return ServiceFactory.getRest(title.wikiSite).getSummaryResponse(
            title = title.prefixedText,
            cacheControl = cacheControl,
            saveHeader = if (isInReadingList(title)) OfflineCacheInterceptor.SAVE_HEADER_SAVE else null,
            langHeader = title.wikiSite.languageCode,
            titleHeader = UriUtil.encodeURL(title.prefixedText)
        )
    }

    private suspend fun fetchWatchStatus(title: PageTitle): WatchStatus {
        return if (WikipediaApp.instance.isOnline && AccountUtil.isLoggedIn) {
            val response = ServiceFactory.get(title.wikiSite).getWatchedStatusWithCategories(title.prefixedText)
            val page = response.query?.firstPage()
            WatchStatus(
                isWatched = page?.watched == true,
                hasWatchlistExpiry = page?.hasWatchlistExpiry() == true,
                myQueryResponse = response
            )
        } else if (WikipediaApp.instance.isOnline && !AccountUtil.isLoggedIn) {
            val response = AnonymousNotificationHelper.observableForAnonUserInfo(title.wikiSite)
            WatchStatus(false, false, response)
        } else {
            WatchStatus(false, false, MwQueryResponse())
        }
    }

    private suspend fun fetchCategories(title: PageTitle, watchResponse: MwQueryResponse): List<Category> {
        return if (WikipediaApp.instance.isOnline) {
            val response = ServiceFactory.get(title.wikiSite).getCategoriesProps(title.text)
            (response.query ?: watchResponse.query)?.firstPage()?.categories?.map { category ->
                Category(title = category.title, lang = title.wikiSite.languageCode)
            } ?: emptyList()
        } else emptyList()
    }

    private suspend fun isInReadingList(title: PageTitle): Boolean {
        return AppDatabase.instance.readingListPageDao().findPageInAnyList(title) != null
    }
}

data class WatchStatus(
    val isWatched: Boolean,
    val hasWatchlistExpiry: Boolean,
    val myQueryResponse: MwQueryResponse
)

sealed class PageResult {
    data class Success(
        val pageSummaryResponse: Response<PageSummary>,
        val categories: List<Category>,
        val isWatched: Boolean,
        val hasWatchlistExpiry: Boolean,
        val redirectedFrom: String?
    ) : PageResult()

    data class Error(val throwable: Throwable) : PageResult()
}
