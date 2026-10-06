package org.wikipedia.yearinreview.presentation

/** textProperties, imageUrls should hold the binding properties defined by the selected Rive view model. */
data class RiveSlideContent(
    val spec: RiveSlideSpec,
    val textProperties: Map<String, String> = emptyMap(),
    val imageUrls: Map<String, String?> = emptyMap(),
    val accessibilityDescription: String = ""
)
