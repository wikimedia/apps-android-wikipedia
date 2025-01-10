package org.wikipedia.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.wikipedia.theme.ThemeColors

@Immutable
data class WikipediaColor(
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
    primaryColor = ThemeColors.Gray700,
    paperColor = ThemeColors.White,
    backgroundColor = ThemeColors.Gray100,
    inactiveColor = ThemeColors.Gray400,
    placeholderColor = ThemeColors.Gray500,
    secondaryColor = ThemeColors.Gray600,
    borderColor = ThemeColors.Gray200,
    progressiveColor = ThemeColors.Blue600,
    successColor = ThemeColors.Green700,
    destructiveColor = ThemeColors.Red700,
    warningColor = ThemeColors.Yellow700,
    highlightColor = ThemeColors.Yellow500,
    focusColor = ThemeColors.Orange500,
    additionColor = ThemeColors.Blue300_15,
    overlayColor = ThemeColors.Black_30
)

val DarkColors = WikipediaColor(
    primaryColor = ThemeColors.Gray200,
    paperColor = ThemeColors.Gray700,
    backgroundColor = ThemeColors.Gray675,
    inactiveColor = ThemeColors.Gray500,
    placeholderColor = ThemeColors.Gray400,
    secondaryColor = ThemeColors.Gray300,
    borderColor = ThemeColors.Gray650,
    progressiveColor = ThemeColors.Blue300,
    successColor = ThemeColors.Green600,
    destructiveColor = ThemeColors.Red500,
    warningColor = ThemeColors.Orange500,
    highlightColor = ThemeColors.Yellow500_40,
    focusColor = ThemeColors.Orange500_50,
    additionColor = ThemeColors.Blue600_30,
    overlayColor = ThemeColors.Black_70
)

val BlackColors = WikipediaColor(
    primaryColor = ThemeColors.Gray200,
    paperColor = ThemeColors.Black,
    backgroundColor = ThemeColors.Gray700,
    inactiveColor = ThemeColors.Gray500,
    placeholderColor = ThemeColors.Gray500,
    secondaryColor = ThemeColors.Gray300,
    borderColor = ThemeColors.Gray675,
    progressiveColor = ThemeColors.Blue300,
    successColor = ThemeColors.Green600,
    destructiveColor = ThemeColors.Red500,
    warningColor = ThemeColors.Orange500,
    highlightColor = ThemeColors.Yellow500_40,
    focusColor = ThemeColors.Orange500_50,
    additionColor = ThemeColors.Blue600_30,
    overlayColor = ThemeColors.Black_70
)

val SepiaColors = WikipediaColor(
    primaryColor = ThemeColors.Gray700,
    paperColor = ThemeColors.Beige100,
    backgroundColor = ThemeColors.Beige300,
    inactiveColor = ThemeColors.Taupe200,
    placeholderColor = ThemeColors.Taupe600,
    secondaryColor = ThemeColors.Gray600,
    borderColor = ThemeColors.Beige400,
    progressiveColor = ThemeColors.Blue600,
    successColor = ThemeColors.Gray700,
    destructiveColor = ThemeColors.Red700,
    warningColor = ThemeColors.Yellow700,
    highlightColor = ThemeColors.Yellow500,
    focusColor = ThemeColors.Orange500,
    additionColor = ThemeColors.Blue300_15,
    overlayColor = ThemeColors.Black_30
)
