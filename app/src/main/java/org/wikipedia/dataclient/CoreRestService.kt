package org.wikipedia.dataclient

import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Revision
import org.wikipedia.readinglist.sync.SyncedReadingLists
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CoreRestService {

    @GET("v1/revision/{oldRev}/compare/{newRev}")
    suspend fun getDiff(
        @Path("oldRev") oldRev: Long,
        @Path("newRev") newRev: Long
    ): DiffResponse

    @GET("v1/page/{title}/history/counts/{editType}")
    suspend fun getEditCount(
            @Path("title") title: String,
            @Path("editType") editType: String
    ): EditCount

    @GET("v1/revision/{rev}")
    suspend fun getRevision(
        @Path("rev") rev: Long
    ): Revision

    @PUT("growthexperiments/v0/suggestions/addimage/feedback/{title}")
    suspend fun addImageFeedback(
        @Path("title") title: String,
        @Body body: GrowthImageSuggestion.AddImageFeedbackBody
    )

    // ------- Reading lists -------
    @POST("readinglists/v0/lists/setup")
    fun setupReadingLists(@Query("csrf_token") token: String?): Call<Unit>

    @POST("readinglists/v0/lists/teardown")
    fun tearDownReadingLists(@Query("csrf_token") token: String?): Call<Unit>

    @Headers("Cache-Control: no-cache")
    @GET("readinglists/v0/lists")
    fun getReadingLists(@Query("next") next: String?): Call<SyncedReadingLists>

    @POST("readinglists/v0/lists")
    fun createReadingList(
        @Query("csrf_token") token: String?,
        @Body list: SyncedReadingLists.RemoteReadingList?
    ): Call<SyncedReadingLists.RemoteIdResponse>

    @PUT("readinglists/v0/lists/{id}")
    fun updateReadingList(
        @Path("id") listId: Long, @Query("csrf_token") token: String?,
        @Body list: SyncedReadingLists.RemoteReadingList?
    ): Call<Unit>

    @DELETE("readinglists/v0/lists/{id}")
    fun deleteReadingList(
        @Path("id") listId: Long,
        @Query("csrf_token") token: String?
    ): Call<Unit>

    @Headers("Cache-Control: no-cache")
    @GET("readinglists/v0/lists/changes/since/{date}")
    fun getReadingListChangesSince(
        @Path("date") iso8601Date: String?,
        @Query("next") next: String?
    ): Call<SyncedReadingLists>

    @Headers("Cache-Control: no-cache")
    @GET("readinglists/v0/lists/pages/{project}/{title}")
    fun getReadingListsContaining(
        @Path("project") project: String?,
        @Path("title") title: String?,
        @Query("next") next: String?
    ): Call<SyncedReadingLists>

    @Headers("Cache-Control: no-cache")
    @GET("readinglists/v0/lists/{id}/entries")
    fun getReadingListEntries(
        @Path("id") listId: Long,
        @Query("next") next: String?
    ): Call<SyncedReadingLists>

    @POST("readinglists/v0/lists/{id}/entries")
    fun addEntryToReadingList(
        @Path("id") listId: Long,
        @Query("csrf_token") token: String?,
        @Body entry: SyncedReadingLists.RemoteReadingListEntry?
    ): Call<SyncedReadingLists.RemoteIdResponse>

    @POST("readinglists/v0/lists/{id}/entries/batch")
    fun addEntriesToReadingList(
        @Path("id") listId: Long,
        @Query("csrf_token") token: String?,
        @Body batch: SyncedReadingLists.RemoteReadingListEntryBatch?
    ): Call<SyncedReadingLists.RemoteIdResponseBatch>

    @DELETE("readinglists/v0/lists/{id}/entries/{entry_id}")
    fun deleteEntryFromReadingList(
        @Path("id") listId: Long,
        @Path("entry_id") entryId: Long,
        @Query("csrf_token") token: String?
    ): Call<Unit>

    companion object {
        const val CORE_REST_API_PREFIX = "w/rest.php/"
    }
}
