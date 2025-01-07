package org.wikipedia.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WikipediaColorSystem(
    val primaryColor: Color,
    val paperColor: Color,
    val backgroundColor: Color,
    val inactiveColor: Color,
    val placeholderColor: Color,
    val secondaryColor: Color,
    val borderColor: Color,
    val progressiveColor: Color,
    val successColor: Color,
    val destructiveColor: Color,
    val warningColor: Color,
    val highlightColor: Color,
    val focusColor: Color,
    val additionColor: Color,
    val overlayColor: Color
)

val LocalWikipediaColorSystem = staticCompositionLocalOf {
    WikipediaColorSystem(
        primaryColor = Color.Unspecified,
        paperColor = Color.Unspecified,
        backgroundColor = Color.Unspecified,
        inactiveColor = Color.Unspecified,
        placeholderColor = Color.Unspecified,
        secondaryColor = Color.Unspecified,
        borderColor = Color.Unspecified,
        progressiveColor = Color.Unspecified,
        successColor = Color.Unspecified,
        destructiveColor = Color.Unspecified,
        warningColor = Color.Unspecified,
        highlightColor = Color.Unspecified,
        focusColor = Color.Unspecified,
        additionColor = Color.Unspecified,
        overlayColor = Color.Unspecified,
    )
}
