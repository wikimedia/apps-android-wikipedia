package org.wikipedia.dataclient

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
import org.wikipedia.readinglist.sync.SyncedReadingLists
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteIdResponse
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteIdResponseBatch
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingList
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntry
import org.wikipedia.readinglist.sync.SyncedReadingLists.RemoteReadingListEntryBatch
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RestService {

    @Headers("x-analytics: preview=1", "Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/summary/{title}")
    suspend fun getPageSummary(
        @Path("title") title: String,
        @Header("Referer") referrerUrl: String? = null,
        @Header("Cache-Control") cacheControl: String? = null,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String? = null,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String? = null,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String? = null
    ): PageSummary

    @Headers("x-analytics: preview=1", "Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/summary/{title}")
    suspend fun getSummaryResponse(
        @Path("title") title: String,
        @Header("Referer") referrerUrl: String? = null,
        @Header("Cache-Control") cacheControl: String? = null,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String? = null,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String? = null,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String? = null
    ): Response<PageSummary>

    @Headers("Accept: $ACCEPT_HEADER_DEFINITION")
    @GET("page/definition/{title}")
    suspend fun getDefinition(@Path("title") title: String): Map<String, List<RbDefinition.Usage>>

    @GET("page/random/summary")
    @Headers("Accept: $ACCEPT_HEADER_SUMMARY")
    suspend fun getRandomSummary(): PageSummary

    @GET("page/media-list/{title}")
    suspend fun getMediaList(
        @Path("title") title: String,
        @Header("Cache-Control") cacheControl: String? = null,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String? = null,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String? = null,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String? = null
    ): MediaList

    @GET("page/media-list/{title}/{revision}")
    suspend fun getMediaList(
        @Path("title") title: String,
        @Path("revision") revision: Long
    ): MediaList

    @GET("feed/onthisday/events/{mm}/{dd}")
    suspend fun getOnThisDay(@Path("mm") month: Int,
                             @Path("dd") day: Int): OnThisDay

    // TODO: Remove this before next fundraising campaign in 2024
    @GET("feed/announcements")
    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "announcements/0.1.0\"")
    suspend fun getAnnouncements(): AnnouncementList

    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "aggregated-feed/0.5.0\"")
    @GET("feed/featured/{year}/{month}/{day}")
    suspend fun getFeedFeatured(
        @Path("year") year: String?,
        @Path("month") month: String?,
        @Path("day") day: String?,
        @Query("lang") lang: String?
    ): AggregatedFeedContent

    @GET("feed/availability")
    suspend fun feedAvailability(): FeedAvailability

    // ------- Reading lists -------
    @POST("data/lists/setup")
    fun setupReadingLists(@Query("csrf_token") token: String?): Call<Unit>

    @POST("data/lists/teardown")
    fun tearDownReadingLists(@Query("csrf_token") token: String?): Call<Unit>

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/")
    fun getReadingLists(@Query("next") next: String?): Call<SyncedReadingLists>

    @POST("data/lists/")
    fun createReadingList(
        @Query("csrf_token") token: String?,
        @Body list: RemoteReadingList?
    ): Call<RemoteIdResponse>

    @Headers("Cache-Control: no-cache")
    @PUT("data/lists/{id}")
    fun updateReadingList(
        @Path("id") listId: Long, @Query("csrf_token") token: String?,
        @Body list: RemoteReadingList?
    ): Call<Unit>

    @Headers("Cache-Control: no-cache")
    @DELETE("data/lists/{id}")
    fun deleteReadingList(
        @Path("id") listId: Long,
        @Query("csrf_token") token: String?
    ): Call<Unit>

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/changes/since/{date}")
    fun getReadingListChangesSince(
        @Path("date") iso8601Date: String?,
        @Query("next") next: String?
    ): Call<SyncedReadingLists>

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/pages/{project}/{title}")
    fun getReadingListsContaining(
        @Path("project") project: String?,
        @Path("title") title: String?,
        @Query("next") next: String?
    ): Call<SyncedReadingLists>

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/{id}/entries/")
    fun getReadingListEntries(
        @Path("id") listId: Long,
        @Query("next") next: String?
    ): Call<SyncedReadingLists>

    @POST("data/lists/{id}/entries/")
    fun addEntryToReadingList(
        @Path("id") listId: Long,
        @Query("csrf_token") token: String?,
        @Body entry: RemoteReadingListEntry?
    ): Call<RemoteIdResponse>

    @POST("data/lists/{id}/entries/batch")
    fun addEntriesToReadingList(
        @Path("id") listId: Long,
        @Query("csrf_token") token: String?,
        @Body batch: RemoteReadingListEntryBatch?
    ): Call<RemoteIdResponseBatch>

    @Headers("Cache-Control: no-cache")
    @DELETE("data/lists/{id}/entries/{entry_id}")
    fun deleteEntryFromReadingList(
        @Path("id") listId: Long, @Path("entry_id") entryId: Long,
        @Query("csrf_token") token: String?
    ): Call<Unit>

    //  ------- Talk pages -------
    @Headers("Cache-Control: no-cache")
    @GET("page/talk/{title}")
    suspend fun getTalkPage(@Path("title") title: String?): TalkPage

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
