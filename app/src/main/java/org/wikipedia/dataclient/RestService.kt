package org.wikipedia.dataclient

import io.reactivex.rxjava3.core.Observable
import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.dataclient.restbase.RbDefinition
import org.wikipedia.dataclient.restbase.RbRelatedPages
import org.wikipedia.feed.aggregated.AggregatedFeedContent
import org.wikipedia.feed.announcement.AnnouncementList
import org.wikipedia.feed.configure.FeedAvailability
import org.wikipedia.feed.onthisday.OnThisDay
import org.wikipedia.gallery.MediaList
import org.wikipedia.readinglist.sync.SyncedReadingLists
import org.wikipedia.readinglist.sync.SyncedReadingLists.*
import org.wikipedia.suggestededits.provider.SuggestedEditItem
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

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

    /**
     * Gets a page summary for a given title -- for link previews
     *
     * @param title the page title to be used including prefix
     */
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

    // todo: this Content Service-only endpoint is under page/ but that implementation detail should
    //       probably not be reflected here. Move to WordDefinitionClient
    /**
     * Gets selected Wiktionary content for a given title derived from user-selected text
     *
     * @param title the Wiktionary page title derived from user-selected Wikipedia article text
     */
    @Headers("Accept: $ACCEPT_HEADER_DEFINITION")
    @GET("page/definition/{title}")
    fun getDefinition(@Path("title") title: String): Observable<Map<String, List<RbDefinition.Usage>>>

    @get:GET("page/random/summary")
    @get:Headers("Accept: $ACCEPT_HEADER_SUMMARY")
    val randomSummary: Observable<PageSummary>

    @Headers("Accept: $ACCEPT_HEADER_SUMMARY")
    @GET("page/related/{title}")
    fun getRelatedPages(@Path("title") title: String?): Observable<RbRelatedPages>

    @GET("page/media-list/{title}/{revision}")
    fun getMediaList(
        @Path("title") title: String,
        @Path("revision") revision: Long
    ): Observable<MediaList>

    @GET("page/media-list/{title}/{revision}")
    suspend fun getMediaListResponse(
        @Path("title") title: String?,
        @Path("revision") revision: Long,
        @Header("Cache-Control") cacheControl: String?,
        @Header(OfflineCacheInterceptor.SAVE_HEADER) saveHeader: String?,
        @Header(OfflineCacheInterceptor.LANG_HEADER) langHeader: String?,
        @Header(OfflineCacheInterceptor.TITLE_HEADER) titleHeader: String?
    ): Response<MediaList>

    @GET("feed/onthisday/events/{mm}/{dd}")
    fun getOnThisDay(@Path("mm") month: Int, @Path("dd") day: Int): Observable<OnThisDay>

    @get:GET("feed/announcements")
    @get:Headers("Accept: " + ACCEPT_HEADER_PREFIX + "announcements/0.1.0\"")
    val announcements: Observable<AnnouncementList>

    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "aggregated-feed/0.5.0\"")
    @GET("feed/featured/{year}/{month}/{day}")
    fun getAggregatedFeed(
        @Path("year") year: String?,
        @Path("month") month: String?,
        @Path("day") day: String?
    ): Observable<AggregatedFeedContent>

    @get:GET("feed/availability")
    val feedAvailability: Observable<FeedAvailability>

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
