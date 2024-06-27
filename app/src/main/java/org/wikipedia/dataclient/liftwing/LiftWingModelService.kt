package org.wikipedia.dataclient.liftwing

import retrofit2.http.Body
import retrofit2.http.POST

interface LiftWingModelService {

    @POST("models/article-descriptions:predict")
    suspend fun getDescriptionSuggestion(
        @Body body: DescriptionSuggestion.Request,
    ): DescriptionSuggestion.Response

    companion object {
        const val API_URL = "https://api.wikimedia.org/service/lw/inference/v1/"
    }
}
