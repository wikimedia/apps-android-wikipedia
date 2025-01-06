package org.wikipedia.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class WikipediaThemeType {
    SYSTEM, LIGHT, DARK, BLACK, SEPIA
}

object WikipediaTheme {
    fun lightColors() = WikipediaColorSystem(
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

    fun darkColors() = WikipediaColorSystem(
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

    fun blackColors() = WikipediaColorSystem(
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

    fun sepiaColors() = WikipediaColorSystem(
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
    wikipediaThemeType: WikipediaThemeType = WikipediaThemeType.SYSTEM,
    content: @Composable () -> Unit
) {
    val wikipediaColorSystem = when (wikipediaThemeType) {
        WikipediaThemeType.LIGHT -> WikipediaTheme.lightColors()
        WikipediaThemeType.DARK -> WikipediaTheme.darkColors()
        WikipediaThemeType.BLACK -> WikipediaTheme.blackColors()
        WikipediaThemeType.SEPIA -> WikipediaTheme.sepiaColors()
        WikipediaThemeType.SYSTEM -> if (isSystemInDarkTheme()) {
            WikipediaTheme.darkColors()
        } else WikipediaTheme.lightColors()
    }

//    val colorScheme = ColorScheme(
//        primary = wikipediaColorSystem.progressiveColor,
//        onPrimary = wikipediaColorSystem.primaryColor,
//        primaryContainer = wikipediaColorSystem.paperColor,
//        onPrimaryContainer = wikipediaColorSystem.primaryColor,
//        inversePrimary = wikipediaColorSystem.progressiveColor,
//        secondary = wikipediaColorSystem.secondaryColor,
//        onSecondary = wikipediaColorSystem.primaryColor,
//        secondaryContainer = wikipediaColorSystem.secondaryColor,
//        onSecondaryContainer = wikipediaColorSystem.secondaryColor,
//        tertiary = wikipediaColorSystem.inactiveColor,
//        onTertiary = wikipediaColorSystem.secondaryColor,
//        tertiaryContainer = wikipediaColorSystem.paperColor,
//        onTertiaryContainer = wikipediaColorSystem.placeholderColor,
//        background = wikipediaColorSystem.backgroundColor,
//        onBackground = wikipediaColorSystem.primaryColor,
//        surface = wikipediaColorSystem.paperColor,
//        onSurface = wikipediaColorSystem.primaryColor,
//        surfaceVariant = wikipediaColorSystem.paperColor,
//        onSurfaceVariant = wikipediaColorSystem.primaryColor,
//        surfaceTint = wikipediaColorSystem.primaryColor,
//        inverseSurface = wikipediaColorSystem.borderColor,
//        inverseOnSurface = wikipediaColorSystem.primaryColor,
//        error = wikipediaColorSystem.destructiveColor,
//        onError = wikipediaColorSystem.destructiveColor,
//        errorContainer = wikipediaColorSystem.destructiveColor,
//        onErrorContainer = wikipediaColorSystem.destructiveColor,
//        outline = wikipediaColorSystem.borderColor,
//        outlineVariant = wikipediaColorSystem.borderColor,
//        scrim = wikipediaColorSystem.overlayColor,
//        surfaceBright = wikipediaColorSystem.primaryColor,
//        surfaceDim = wikipediaColorSystem.primaryColor,
//        surfaceContainer = wikipediaColorSystem.primaryColor,
//        surfaceContainerHigh = wikipediaColorSystem.primaryColor,
//        surfaceContainerHighest = wikipediaColorSystem.primaryColor,
//        surfaceContainerLow = wikipediaColorSystem.primaryColor,
//        surfaceContainerLowest = wikipediaColorSystem.primaryColor
//    )

    CompositionLocalProvider(
        LocalWikipediaColorSystem provides wikipediaColorSystem
    ) {
        content()
    }

//    MaterialTheme(
//        colorScheme = colorScheme,
//        content = content
//    )
}

object NewTheme {
    val colors: WikipediaColorSystem
        @Composable
        get() = LocalWikipediaColorSystem.current
}
