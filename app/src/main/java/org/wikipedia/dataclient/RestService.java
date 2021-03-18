package org.wikipedia.dataclient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.okhttp.OfflineCacheInterceptor;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.page.TalkPage;
import org.wikipedia.dataclient.restbase.RbDefinition;
import org.wikipedia.dataclient.restbase.RbRelatedPages;
import org.wikipedia.feed.aggregated.AggregatedFeedContent;
import org.wikipedia.feed.announcement.AnnouncementList;
import org.wikipedia.feed.configure.FeedAvailability;
import org.wikipedia.feed.onthisday.OnThisDay;
import org.wikipedia.gallery.MediaList;
import org.wikipedia.readinglist.sync.SyncedReadingLists;
import org.wikipedia.suggestededits.provider.SuggestedEditItem;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RestService {
    String REST_API_PREFIX = "/api/rest_v1";

    String ACCEPT_HEADER_PREFIX = "application/json; charset=utf-8; profile=\"https://www.mediawiki.org/wiki/Specs/";
    String ACCEPT_HEADER_SUMMARY = ACCEPT_HEADER_PREFIX + "Summary/1.2.0\"";
    String ACCEPT_HEADER_DEFINITION = ACCEPT_HEADER_PREFIX + "definition/0.7.2\"";
    String ACCEPT_HEADER_MOBILE_HTML = ACCEPT_HEADER_PREFIX + "Mobile-HTML/1.2.1\"";

    String PAGE_HTML_ENDPOINT = "page/mobile-html/";
    String PAGE_HTML_PREVIEW_ENDPOINT = "transform/wikitext/to/mobile-html/";

    /**
     * Gets a page summary for a given title -- for link previews
     *
     * @param title the page title to be used including prefix
     */
    @Headers({
            "x-analytics: preview=1",
            "Accept: " + ACCEPT_HEADER_SUMMARY
    })
    @GET("page/summary/{title}")
    @NonNull
    Observable<Response<PageSummary>> getSummaryResponse(@NonNull @Path("title") String title,
                                                         @Nullable @Header("Referer") String referrerUrl,
                                                         @Nullable @Header("Cache-Control") String cacheControl,
                                                         @Nullable @Header(OfflineCacheInterceptor.SAVE_HEADER) String saveHeader,
                                                         @Nullable @Header(OfflineCacheInterceptor.LANG_HEADER) String langHeader,
                                                         @Nullable @Header(OfflineCacheInterceptor.TITLE_HEADER) String titleHeader);

    @Headers({
            "x-analytics: preview=1",
            "Accept: " + ACCEPT_HEADER_SUMMARY
    })
    @GET("page/summary/{title}")
    @NonNull
    Observable<PageSummary> getSummary(@Nullable @Header("Referer") String referrerUrl,
                                       @NonNull @Path("title") String title);

    // todo: this Content Service-only endpoint is under page/ but that implementation detail should
    //       probably not be reflected here. Move to WordDefinitionClient
    /**
     * Gets selected Wiktionary content for a given title derived from user-selected text
     *
     * @param title the Wiktionary page title derived from user-selected Wikipedia article text
     */
    @Headers("Accept: " + ACCEPT_HEADER_DEFINITION)
    @GET("page/definition/{title}")
    @NonNull Observable<Map<String, RbDefinition.Usage[]>> getDefinition(@NonNull @Path("title") String title);

    @Headers("Accept: " + ACCEPT_HEADER_SUMMARY)
    @GET("page/random/summary")
    @NonNull Observable<PageSummary> getRandomSummary();

    @Headers("Accept: " + ACCEPT_HEADER_SUMMARY)
    @GET("page/related/{title}")
    @NonNull Observable<RbRelatedPages> getRelatedPages(@Path("title") String title);

    @GET("page/media-list/{title}/{revision}")
    @NonNull Observable<MediaList> getMediaList(@NonNull @Path("title") String title,
                                                @Path("revision") long revision);

    @GET("page/media-list/{title}/{revision}")
    @NonNull Observable<Response<MediaList>> getMediaListResponse(@Path("title") String title,
                                                                  @Path("revision") long revision,
                                                                  @Nullable @Header("Cache-Control") String cacheControl,
                                                                  @Nullable @Header(OfflineCacheInterceptor.SAVE_HEADER) String saveHeader,
                                                                  @Nullable @Header(OfflineCacheInterceptor.LANG_HEADER) String langHeader,
                                                                  @Nullable @Header(OfflineCacheInterceptor.TITLE_HEADER) String titleHeader);

    @GET("feed/onthisday/events/{mm}/{dd}")
    @NonNull Observable<OnThisDay> getOnThisDay(@Path("mm") int month, @Path("dd") int day);

    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "announcements/0.1.0\"")
    @GET("feed/announcements")
    @NonNull Observable<AnnouncementList> getAnnouncements();

    @Headers("Accept: " + ACCEPT_HEADER_PREFIX + "aggregated-feed/0.5.0\"")
    @GET("feed/featured/{year}/{month}/{day}")
    @NonNull Observable<AggregatedFeedContent> getAggregatedFeed(@Path("year") String year,
                                                                 @Path("month") String month,
                                                                 @Path("day") String day);

    @GET("feed/availability")
    @NonNull Observable<FeedAvailability> getFeedAvailability();

    // ------- Reading lists -------

    @POST("data/lists/setup")
    @NonNull Call<Void> setupReadingLists(@Query("csrf_token") String token);

    @POST("data/lists/teardown")
    @NonNull Call<Void> tearDownReadingLists(@Query("csrf_token") String token);

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/")
    @NonNull Call<SyncedReadingLists> getReadingLists(@Query("next") String next);

    @POST("data/lists/")
    @NonNull Call<SyncedReadingLists.RemoteIdResponse> createReadingList(@Query("csrf_token") String token,
                                                                         @Body SyncedReadingLists.RemoteReadingList list);

    @Headers("Cache-Control: no-cache")
    @PUT("data/lists/{id}")
    @NonNull Call<Void> updateReadingList(@Path("id") long listId, @Query("csrf_token") String token,
                                          @Body SyncedReadingLists.RemoteReadingList list);

    @Headers("Cache-Control: no-cache")
    @DELETE("data/lists/{id}")
    @NonNull Call<Void> deleteReadingList(@Path("id") long listId, @Query("csrf_token") String token);

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/changes/since/{date}")
    @NonNull Call<SyncedReadingLists> getReadingListChangesSince(@Path("date") String iso8601Date,
                                                                 @Query("next") String next);

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/pages/{project}/{title}")
    @NonNull Call<SyncedReadingLists> getReadingListsContaining(@Path("project") String project,
                                                                @Path("title") String title,
                                                                @Query("next") String next);

    @Headers("Cache-Control: no-cache")
    @GET("data/lists/{id}/entries/")
    @NonNull Call<SyncedReadingLists> getReadingListEntries(@Path("id") long listId, @Query("next") String next);

    @POST("data/lists/{id}/entries/")
    @NonNull Call<SyncedReadingLists.RemoteIdResponse> addEntryToReadingList(@Path("id") long listId,
                                                                             @Query("csrf_token") String token,
                                                                             @Body SyncedReadingLists.RemoteReadingListEntry entry);

    @POST("data/lists/{id}/entries/batch")
    @NonNull Call<SyncedReadingLists.RemoteIdResponseBatch> addEntriesToReadingList(@Path("id") long listId,
                                                                                    @Query("csrf_token") String token,
                                                                                    @Body SyncedReadingLists.RemoteReadingListEntryBatch batch);

    @Headers("Cache-Control: no-cache")
    @DELETE("data/lists/{id}/entries/{entry_id}")
    @NonNull Call<Void> deleteEntryFromReadingList(@Path("id") long listId, @Path("entry_id") long entryId,
                                                   @Query("csrf_token") String token);


    // ------- Recommendations -------

    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/caption/addition/{lang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getImagesWithoutCaptions(@NonNull @Path("lang") String lang);

    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/caption/translation/from/{fromLang}/to/{toLang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getImagesWithTranslatableCaptions(@NonNull @Path("fromLang") String fromLang,
                                                                          @NonNull @Path("toLang") String toLang);
    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/description/addition/{lang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getArticlesWithoutDescriptions(@NonNull @Path("lang") String lang);

    @Headers("Cache-Control: no-cache")
    @GET("data/recommendation/description/translation/from/{fromLang}/to/{toLang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getArticlesWithTranslatableDescriptions(@NonNull @Path("fromLang") String fromLang,
                                                                                @NonNull @Path("toLang") String toLang);

    //  ------- Talk pages -------

    @Headers("Cache-Control: no-cache")
    @GET("page/talk/{title}")
    @NonNull
    Observable<TalkPage> getTalkPage(@Nullable @Path("title") String title);

}
