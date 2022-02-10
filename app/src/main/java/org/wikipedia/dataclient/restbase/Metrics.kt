package org.wikipedia.dataclient.restbase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
class Metrics {

    val items: List<Items> = emptyList()
    val firstItem = items.first()

    @Serializable
    class Items {
        val project: String? = null
        @SerialName("editor-type") val editorType: String? = null
        @SerialName("page-title") val pageTitle: String? = null
        val granularity: String? = null
        val results: List<Results> = emptyList()
    }

    @Serializable
    class Results {
        val timestamp: String? = null
        val edits: Int? = null
    }
}
