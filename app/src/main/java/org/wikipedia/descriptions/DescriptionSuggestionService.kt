package org.wikipedia.descriptions

import io.reactivex.rxjava3.core.Observable
import retrofit2.http.GET
import retrofit2.http.Query

interface DescriptionSuggestionService {
    @GET("article")
    fun getSuggestion(
        @Query("lang") lang: String,
        @Query("title") title: String,
        @Query("num_beams") count: Int
    ): Observable<DescriptionSuggestionResponse>

    companion object {
        const val API_URL = "https://ml-article-description-api.wmcloud.org/"
    }
}
