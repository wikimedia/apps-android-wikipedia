package org.wikipedia.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class WikipediaThemeType {
    LIGHT, DARK, BLACK, SEPIA
}

object WikipediaTheme {
    fun lightColors() = WikipediaColorScheme(
        paperColor = WikipediaColors.White,
        backgroundColor = WikipediaColors.Gray100,
        borderColor = WikipediaColors.Gray200,
        inactiveColor = WikipediaColors.Gray400,
        placeholderColor = WikipediaColors.Gray500,
        secondaryColor = WikipediaColors.Gray600,
        primaryColor = WikipediaColors.Gray700,
        progressiveColor = WikipediaColors.Blue600,
        successColor = WikipediaColors.Green700,
        destructiveColor = WikipediaColors.Red700,
        warningColor = WikipediaColors.Yellow700,
        highlightColor = WikipediaColors.Yellow500,
        focusColor = WikipediaColors.Orange500,
        additionColor = WikipediaColors.Blue300_15,
        overlayColor = WikipediaColors.Black_30
    )

    fun darkColors() = WikipediaColorScheme(
        paperColor = WikipediaColors.Gray700,
        backgroundColor = WikipediaColors.Gray675,
        borderColor = WikipediaColors.Gray650,
        inactiveColor = WikipediaColors.Gray500,
        placeholderColor = WikipediaColors.Gray400,
        secondaryColor = WikipediaColors.Gray300,
        primaryColor = WikipediaColors.Gray200,
        progressiveColor = WikipediaColors.Blue300,
        successColor = WikipediaColors.Green600,
        destructiveColor = WikipediaColors.Red500,
        warningColor = WikipediaColors.Orange500,
        highlightColor = WikipediaColors.Yellow500_40,
        focusColor = WikipediaColors.Orange500_50,
        additionColor = WikipediaColors.Blue600_30,
        overlayColor = WikipediaColors.Black_70
    )

    fun blackColors() = WikipediaColorScheme(
        paperColor = WikipediaColors.Black,
        backgroundColor = WikipediaColors.Gray700,
        borderColor = WikipediaColors.Gray675,
        inactiveColor = WikipediaColors.Gray500,
        placeholderColor = WikipediaColors.Gray500,
        secondaryColor = WikipediaColors.Gray300,
        primaryColor = WikipediaColors.Gray200,
        progressiveColor = WikipediaColors.Blue300,
        successColor = WikipediaColors.Green600,
        destructiveColor = WikipediaColors.Red500,
        warningColor = WikipediaColors.Orange500,
        highlightColor = WikipediaColors.Yellow500_40,
        focusColor = WikipediaColors.Orange500_50,
        additionColor = WikipediaColors.Blue600_30,
        overlayColor = WikipediaColors.Black_70
    )

    fun sepiaColors() = WikipediaColorScheme(
        paperColor = WikipediaColors.Beige100,
        backgroundColor = WikipediaColors.Beige300,
        borderColor = WikipediaColors.Beige400,
        inactiveColor = WikipediaColors.Taupe200,
        placeholderColor = WikipediaColors.Taupe600,
        secondaryColor = WikipediaColors.Gray600,
        primaryColor = WikipediaColors.Gray700,
        progressiveColor = WikipediaColors.Blue600,
        successColor = WikipediaColors.Gray700,
        destructiveColor = WikipediaColors.Red700,
        warningColor = WikipediaColors.Yellow700,
        highlightColor = WikipediaColors.Yellow500,
        focusColor = WikipediaColors.Orange500,
        additionColor = WikipediaColors.Blue300_15,
        overlayColor = WikipediaColors.Black_30
    )
}

@Composable
fun MainTheme(
    wikipediaThemeType: WikipediaThemeType,
    content: @Composable () -> Unit
) {
    val appColors = when (wikipediaThemeType) {
        WikipediaThemeType.LIGHT -> WikipediaTheme.lightColors()
        WikipediaThemeType.DARK -> WikipediaTheme.darkColors()
        WikipediaThemeType.BLACK -> WikipediaTheme.blackColors()
        WikipediaThemeType.SEPIA -> WikipediaTheme.sepiaColors()
    }

    val colorScheme = ColorScheme(
        primary = appColors.progressiveColor,
        onPrimary = appColors.primaryColor,
        primaryContainer = appColors.paperColor,
        onPrimaryContainer = appColors.primaryColor,
        inversePrimary = appColors.progressiveColor,
        secondary = appColors.secondaryColor,
        onSecondary = appColors.primaryColor,
        secondaryContainer = appColors.secondaryColor,
        onSecondaryContainer = appColors.secondaryColor,
        tertiary = appColors.inactiveColor,
        onTertiary = appColors.secondaryColor,
        tertiaryContainer = appColors.paperColor,
        onTertiaryContainer = appColors.placeholderColor,
        background = appColors.backgroundColor,
        onBackground = appColors.primaryColor,
        surface = appColors.paperColor,
        onSurface = appColors.primaryColor,
        surfaceVariant = appColors.paperColor,
        onSurfaceVariant = appColors.primaryColor,
        surfaceTint = appColors.primaryColor,
        inverseSurface = appColors.borderColor,
        inverseOnSurface = appColors.primaryColor,
        error = appColors.destructiveColor,
        onError = appColors.destructiveColor,
        errorContainer = appColors.destructiveColor,
        onErrorContainer = appColors.destructiveColor,
        outline = appColors.borderColor,
        outlineVariant = appColors.borderColor,
        scrim = appColors.overlayColor,
        surfaceBright = appColors.primaryColor,
        surfaceDim = appColors.primaryColor,
        surfaceContainer = appColors.primaryColor,
        surfaceContainerHigh = appColors.primaryColor,
        surfaceContainerHighest = appColors.primaryColor,
        surfaceContainerLow = appColors.primaryColor,
        surfaceContainerLowest = appColors.primaryColor
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
