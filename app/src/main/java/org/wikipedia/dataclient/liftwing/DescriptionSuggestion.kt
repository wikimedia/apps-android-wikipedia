package org.wikipedia.dataclient.liftwing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DescriptionSuggestion {

    @Serializable
    class Request(
        val lang: String,
        val title: String,
        @SerialName("num_beams") val count: Int = 1
    )

    @Serializable
    class Response {
        val prediction: List<String> = emptyList()
        val blp = false
    }
}
