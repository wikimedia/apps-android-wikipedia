package org.wikipedia.yearinreview.presentation

/**
 * textProperties, numberProperties, booleanProperties, imageUrls should hold the binding properties defined by the selected Rive view model.
 * textProperties should be in the order it appears in the slide.
 */
data class RiveSlideContent(
    val spec: RiveSlideSpec,
    val textProperties: Map<String, String> = emptyMap(),
    val numberProperties: Map<String, Int> = emptyMap(),
    val booleanProperties: Map<String, Boolean> = emptyMap(),
    val imageUrls: Map<String, String?> = emptyMap(),
    val accessibilityDescription: String = textProperties.values.filter { it.isNotBlank() }.joinToString("\n"),
    val accessibilityActions: List<RiveAccessibilityAction> = emptyList(),
    // Replaces this content on the same slide once the spec's frameCompleteTrigger fires
    val revealContent: RiveSlideContent? = null
)

// Lets screen reader users do what tapping an element of the artboard does, by setting the view model boolean that the tap sets to true
data class RiveAccessibilityAction(
    val label: String,
    val tappedBooleanProperty: String
)
