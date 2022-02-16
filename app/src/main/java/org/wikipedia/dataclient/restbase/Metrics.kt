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
        val project: String = ""
        @SerialName("editor-type") val editorType: String = ""
        @SerialName("page-title") val pageTitle: String = ""
        val granularity: String = ""
        val results: List<Results> = emptyList()
    }

    @Serializable
    class Results {
        val timestamp: String = ""
        val edits: Int = 0
    }
}
