package org.wikipedia.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.wikipedia.compose.ComposeColors

@Immutable
data class WikipediaColor(
    val isDarkTheme: Boolean = false,
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

val LocalWikipediaColor = staticCompositionLocalOf {
    WikipediaColor(
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

val LightColors = WikipediaColor(
    primaryColor = ComposeColors.Gray700,
    paperColor = ComposeColors.White,
    backgroundColor = ComposeColors.Gray100,
    inactiveColor = ComposeColors.Gray400,
    placeholderColor = ComposeColors.Gray500,
    secondaryColor = ComposeColors.Gray600,
    borderColor = ComposeColors.Gray200,
    progressiveColor = ComposeColors.Blue600,
    successColor = ComposeColors.Green700,
    destructiveColor = ComposeColors.Red700,
    warningColor = ComposeColors.Yellow700,
    highlightColor = ComposeColors.Yellow500,
    focusColor = ComposeColors.Orange500,
    additionColor = ComposeColors.Blue300_15,
    overlayColor = ComposeColors.Black_30
)

val DarkColors = WikipediaColor(
    isDarkTheme = true,
    primaryColor = ComposeColors.Gray200,
    paperColor = ComposeColors.Gray700,
    backgroundColor = ComposeColors.Gray675,
    inactiveColor = ComposeColors.Gray500,
    placeholderColor = ComposeColors.Gray400,
    secondaryColor = ComposeColors.Gray300,
    borderColor = ComposeColors.Gray650,
    progressiveColor = ComposeColors.Blue300,
    successColor = ComposeColors.Green600,
    destructiveColor = ComposeColors.Red500,
    warningColor = ComposeColors.Orange500,
    highlightColor = ComposeColors.Yellow500_40,
    focusColor = ComposeColors.Orange500_50,
    additionColor = ComposeColors.Blue600_30,
    overlayColor = ComposeColors.Black_70
)

val BlackColors = WikipediaColor(
    isDarkTheme = true,
    primaryColor = ComposeColors.Gray200,
    paperColor = ComposeColors.Black,
    backgroundColor = ComposeColors.Gray700,
    inactiveColor = ComposeColors.Gray500,
    placeholderColor = ComposeColors.Gray500,
    secondaryColor = ComposeColors.Gray300,
    borderColor = ComposeColors.Gray675,
    progressiveColor = ComposeColors.Blue300,
    successColor = ComposeColors.Green600,
    destructiveColor = ComposeColors.Red500,
    warningColor = ComposeColors.Orange500,
    highlightColor = ComposeColors.Yellow500_40,
    focusColor = ComposeColors.Orange500_50,
    additionColor = ComposeColors.Blue600_30,
    overlayColor = ComposeColors.Black_70
)

val SepiaColors = WikipediaColor(
    primaryColor = ComposeColors.Gray700,
    paperColor = ComposeColors.Beige100,
    backgroundColor = ComposeColors.Beige300,
    inactiveColor = ComposeColors.Taupe200,
    placeholderColor = ComposeColors.Taupe600,
    secondaryColor = ComposeColors.Gray600,
    borderColor = ComposeColors.Beige400,
    progressiveColor = ComposeColors.Blue600,
    successColor = ComposeColors.Gray700,
    destructiveColor = ComposeColors.Red700,
    warningColor = ComposeColors.Yellow700,
    highlightColor = ComposeColors.Yellow500,
    focusColor = ComposeColors.Orange500,
    additionColor = ComposeColors.Blue300_15,
    overlayColor = ComposeColors.Black_30
)
