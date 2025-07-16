package org.wikipedia.page.pageload

import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.categories.db.Category
import org.wikipedia.database.AppDatabase
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryResponse
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.PageTitle
import org.wikipedia.util.Resource
import org.wikipedia.util.UriUtil
import retrofit2.Response
import java.io.IOException

class PageDataFetcher {

    suspend fun fetchPageSummary(title: PageTitle, cacheControl: String): Response<PageSummary> {
        return ServiceFactory.getRest(title.wikiSite).getSummaryResponse(
            title = title.prefixedText,
            cacheControl = cacheControl,
            saveHeader = if (isInReadingList(title)) OfflineCacheInterceptor.SAVE_HEADER_SAVE else null,
            langHeader = title.wikiSite.languageCode,
            titleHeader = UriUtil.encodeURL(title.prefixedText)
        )
    }

    suspend fun fetchWatchStatus(title: PageTitle): Resource<WatchStatus> {
        try {
            val watchStatus = if (WikipediaApp.instance.isOnline && AccountUtil.isLoggedIn) {
                val response = ServiceFactory.get(title.wikiSite).getWatchedStatusWithCategories(title.prefixedText)
                val page = response.query?.firstPage()
                WatchStatus(
                    isWatched = page?.watched == true,
                    hasWatchlistExpiry = page?.hasWatchlistExpiry() == true,
                    myQueryResponse = response
                )
            } else if (WikipediaApp.instance.isOnline && !AccountUtil.isLoggedIn) {
                val response = AnonymousNotificationHelper.maybeGetAnonUserInfo(title.wikiSite)
                WatchStatus(isWatched = false, hasWatchlistExpiry = false, myQueryResponse = response)
            } else {
                WatchStatus(isWatched = false, hasWatchlistExpiry = false, myQueryResponse = MwQueryResponse())
            }
            return Resource.Success(watchStatus)
        } catch (e: IOException) {
            return Resource.Error(e)
        }
    }

    suspend fun fetchCategories(title: PageTitle, watchResponse: MwQueryResponse): Resource<List<Category>> {
        try {
            val categories = if (WikipediaApp.instance.isOnline) {
                val response = ServiceFactory.get(title.wikiSite).getCategoriesProps(title.text)
                (response.query ?: watchResponse.query)?.firstPage()?.categories?.map { category ->
                    Category(title = category.title, lang = title.wikiSite.languageCode)
                } ?: emptyList()
            } else emptyList()
            return Resource.Success(categories)
        } catch (e: IOException) {
            return Resource.Error(e)
        }
    }

    private suspend fun isInReadingList(title: PageTitle): Boolean {
        return AppDatabase.instance.readingListPageDao().findPageInAnyList(title) != null
    }
}

data class WatchStatus(
    val isWatched: Boolean = false,
    val hasWatchlistExpiry: Boolean = false,
    val myQueryResponse: MwQueryResponse = MwQueryResponse()
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
