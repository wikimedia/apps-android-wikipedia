package org.wikipedia.descriptions

import kotlinx.serialization.Serializable

@Serializable
class DescriptionSuggestionResponse {
    val prediction: List<String> = emptyList()
    val blp = false
}
