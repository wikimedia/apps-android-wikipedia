package org.wikipedia.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class WikipediaColorScheme(
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
