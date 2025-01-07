package org.wikipedia.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class WikipediaThemeType {
    SYSTEM, LIGHT, DARK, BLACK, SEPIA
}

@Composable
fun MainTheme(
    wikipediaThemeType: WikipediaThemeType = WikipediaThemeType.SYSTEM,
    content: @Composable () -> Unit
) {
    val wikipediaColorSystem = when (wikipediaThemeType) {
        WikipediaThemeType.LIGHT -> lightColors
        WikipediaThemeType.DARK -> darkColors
        WikipediaThemeType.BLACK -> blackColors
        WikipediaThemeType.SEPIA -> sepiaColors
        WikipediaThemeType.SYSTEM -> if (isSystemInDarkTheme()) darkColors else lightColors
    }

    CompositionLocalProvider(
        LocalWikipediaColor provides wikipediaColorSystem
    ) {
        content()
    }
}

object WikipediaTheme {
    val colors: WikipediaColor
        @Composable
        get() = LocalWikipediaColor.current
}
