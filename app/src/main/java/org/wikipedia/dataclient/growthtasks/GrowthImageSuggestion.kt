package org.wikipedia.dataclient.growthtasks

import kotlinx.serialization.Serializable

@Suppress("unused")
@Serializable
class GrowthImageSuggestion(
    val titleNamespace: Int = 0,
    val titleText: String = "",
    val datasetId: String = "",
    val images: List<ImageItem> = emptyList(),
) {
    @Serializable
    class ImageItem(
        val image: String = "",
        val displayFilename: String = "",
        val source: String = "",
        val projects: List<String> = emptyList(),
        val metadata: ImageMetadata? = null,
    )

    @Serializable
    class ImageMetadata(
        val descriptionUrl: String = "",
        val thumbUrl: String = "",
        val fullUrl: String = "",
        val originalWidth: Int = 0,
        val originalHeight: Int = 0,
        val mediaType: String = "",
        val description: String = "",
        val author: String = "",
        val license: String = "",
        val date: String = "",
        val caption: String = "",
        val categories: List<String> = emptyList(),
        val reason: String = "",
        val contentLanguageName: String = "",
    )

    @Serializable
    class PluginResponse(
        val taskType: String = "",
        val filename: String = "",
        val accepted: Boolean = false,
        val reasons: List<String> = emptyList(),
        val caption: String = "",
        val sectionNumber: Int? = null,
        val sectionTitle: String? = null
    )
}
