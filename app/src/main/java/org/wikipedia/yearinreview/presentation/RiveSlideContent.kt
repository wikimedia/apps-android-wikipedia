package org.wikipedia.yearinreview.presentation

/**
 * textProperties, numberProperties, imageUrls should hold the binding properties defined by the selected Rive view model.
 * textProperties should be in the order it appears in the slide.
 */
data class RiveSlideContent(
    val spec: RiveSlideSpec,
    val textProperties: Map<String, String> = emptyMap(),
    val numberProperties: Map<String, Int> = emptyMap(),
    val imageUrls: Map<String, String?> = emptyMap(),
    val accessibilityDescription: String = textProperties.values.filter { it.isNotBlank() }.joinToString("\n")
)
