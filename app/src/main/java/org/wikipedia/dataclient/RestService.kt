package org.wikipedia.dataclient

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.dataclient.restbase.Metrics
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.feed.announcement.AnnouncementList
import org.wikipedia.feed.configure.FeedAvailability
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.gallery.MediaList
import org.wikipedia.suggestededits.provider.SuggestedEditItem
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path

interface RestService {

    /**
     * Gets a page summary for a given title -- for link previews
     *
     * @param title the page title to be used including prefix
     */
    @Headers("x-analytics: preview=1", "Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/summary/{title}")
    fun getSummaryResponse(
        @Path("title") title: String,
        @Header("Referer") referrerUrl: String?,
        @Header("Cache-Control") cacheControl: String?,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String?,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String?,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String?
    ): Observable<Response<PageSummary>>

    @Headers("x-analytics: preview=1", "Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/summary/{title}")
    suspend fun getSummaryResponseSuspend(
        @Path("title") title: String,
        @Header("Referer") referrerUrl: String?,
        @Header("Cache-Control") cacheControl: String?,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String?,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String?,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String?
    ): Response<PageSummary>

    @Headers("x-analytics: preview=1", "Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/summary/{title}")
    fun getSummary(
        @Header("Referer") referrerUrl: String?,
        @Path("title") title: String
    ): Observable<PageSummary>

    @Headers("x-analytics: preview=1", "Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/summary/{title}")
    suspend fun getPageSummary(
        @Header("Referer") referrerUrl: String?,
        @Path("title") title: String
    ): PageSummary

    @Headers("Accept: $ACCEPT_HEADER_DEFINITION")
    @GET("page/definition/{title}")
    suspend fun getDefinition(@Path("title") title: String): Map<String, List<RbDefinition.Usage>>

    @GET("page/random/summary")
    @Headers("Accept: $ACCEPT_HEADER_SUMMARY")
    suspend fun getRandomSummary(): PageSummary

    @GET("page/media-list/{title}/{revision}")
    fun getMediaList(
        @Path("title") title: String,
        @Path("revision") revision: Long
    ): Observable<MediaList>

    @GET("page/media-list/{title}/{revision}")
    suspend fun getMediaListSuspend(
        @Path("title") title: String,
        @Path("revision") revision: Long
    ): MediaList

    @GET("page/media-list/{title}/{revision}")
    fun getMediaListResponse(
        @Path("title") title: String?,
        @Path("revision") revision: Long,
        @Header("Cache-Control") cacheControl: String?,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String?,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String?,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String?
    ): Observable<Response<MediaList>>

    @GET("feed/onthisday/events/{mm}/{dd}")
    fun getOnThisDay(@Path("mm") month: Int, @Path("dd") day: Int): Observable<OnThisDay>

    // TODO: Remove this before next fundraising campaign in 2024
    @GET("feed/announcements")
    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "announcements/0.1.0\"")
    suspend fun getAnnouncements(): AnnouncementList

    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "aggregated-feed/0.5.0\"")
    @GET("feed/featured/{year}/{month}/{day}")
    suspend fun getFeedFeatured(
        @Path("year") year: String?,
        @Path("month") month: String?,
        @Path("day") day: String?
    ): AggregatedFeedContent

    @get:GET("feed/availability")
    val feedAvailability: Observable<FeedAvailability>

    // ------- Recommendations -------
    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/caption/addition/{lang}")
    fun getImagesWithoutCaptions(@Path("lang") lang: String): Observable<List<SuggestedEditItem>>

    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/caption/translation/from/{fromLang}/to/{toLang}")
    fun getImagesWithTranslatableCaptions(
        @Path("fromLang") fromLang: String,
        @Path("toLang") toLang: String
    ): Observable<List<SuggestedEditItem>>

    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/description/addition/{lang}")
    fun getArticlesWithoutDescriptions(@Path("lang") lang: String): Observable<List<SuggestedEditItem>>

    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/description/translation/from/{fromLang}/to/{toLang}")
    fun getArticlesWithTranslatableDescriptions(
        @Path("fromLang") fromLang: String,
        @Path("toLang") toLang: String
    ): Observable<List<SuggestedEditItem>>

    //  ------- Talk pages -------
    @Headers("Cache-Control: no-cache")
    @GET("page/talk/{title}")
    fun getTalkPage(@Path("title") title: String?): Observable<TalkPage>

    @Headers("Cache-Control: no-cache")
    @GET("metrics/edits/per-page/{wikiAuthority}/{title}/all-editor-types/monthly/{fromDate}/{toDate}")
    suspend fun getArticleMetrics(
        @Path("wikiAuthority") wikiAuthority: String,
        @Path("title") title: String,
        @Path("fromDate") fromDate: String,
        @Path("toDate") toDate: String
    ): Metrics

    companion object {
        const val REST_API_PREFIX = "/api/rest_v1"
        const val ACCEPT_HEADER_PREFIX = "application/json; charset=utf-8; profile=\"https://www.mediawiki.org/wiki/Specs/"
        const val ACCEPT_HEADER_SUMMARY = ACCEPT_HEADER_PREFIX + "Summary/1.2.0\""
        const val ACCEPT_HEADER_DEFINITION = ACCEPT_HEADER_PREFIX + "definition/0.7.2\""
        const val ACCEPT_HEADER_MOBILE_HTML = ACCEPT_HEADER_PREFIX + "Mobile-HTML/1.2.1\""
        const val PAGE_HTML_ENDPOINT = "page/mobile-html/"
        const val PAGE_HTML_PREVIEW_ENDPOINT = "transform/wikitext/to/mobile-html/"
    }
}
