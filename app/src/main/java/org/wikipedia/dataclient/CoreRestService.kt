package org.wikipedia.dataclient

import org.wikipedia.auth.OAuthProfile
import org.wikipedia.dataclient.growthtasks.GrowthImageSuggestion
import org.wikipedia.dataclient.growthtasks.GrowthUserImpact
import org.wikipedia.dataclient.restbase.DiffResponse
import org.wikipedia.dataclient.restbase.EditCount
import org.wikipedia.dataclient.restbase.Revision
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

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

    @GET("growthexperiments/v0/user-impact/%23{userId}")
    suspend fun getUserImpact(
        @Path("userId") userId: Int
    ): GrowthUserImpact

    @GET("oauth2/resource/profile")
    suspend fun getOAuthProfile(): OAuthProfile

    companion object {
        const val CORE_REST_API_PREFIX = "w/rest.php/"
    }
}
