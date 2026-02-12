package org.wikipedia.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface SemanticSearchService {
    @GET("api/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("k") count: Int,
        @Query("language_code") lang: String,
        @Query("include_text") includeText: Boolean,
    ): SemanticSearchResults

    companion object {
        const val BASE_URL = "https://semantic-search.wmcloud.org/"
    }
}

@Serializable
class SemanticSearchResults {
    @SerialName("language_code") val languageCode: String = ""
    val query: String = ""
    @SerialName("semantic_search_results") val results: List<SemanticSearchResult> = emptyList()
}

@Serializable
class SemanticSearchResult {
    @SerialName("page_title") val title: String = ""
    @SerialName("section_header") val sectionHeader: String = ""
    @SerialName("section_index") val sectionIndex: Int = 0
    @SerialName("section_text") val sectionText: String = ""
    val url: String = ""
}
