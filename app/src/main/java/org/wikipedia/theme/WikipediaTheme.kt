package org.wikipedia.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class WikipediaThemeType {
    SYSTEM, LIGHT, DARK, BLACK, SEPIA
}

// @TODO: add typography, shapes, dimensions
data class WikipediaTheme(
    val colors: WikipediaColor
)

@Composable
fun MainTheme(
    wikipediaThemeType: WikipediaThemeType = WikipediaThemeType.SYSTEM,
    content: @Composable () -> Unit
) {
    val wikipediaColorSystem = when (wikipediaThemeType) {
        WikipediaThemeType.LIGHT -> WikipediaTheme(
            colors = lightColors
        )

        WikipediaThemeType.DARK -> WikipediaTheme(
            colors = darkColors
        )

        WikipediaThemeType.BLACK -> WikipediaTheme(
            colors = blackColors
        )

        WikipediaThemeType.SEPIA -> WikipediaTheme(
            colors = sepiaColors
        )

        WikipediaThemeType.SYSTEM -> if (isSystemInDarkTheme()) WikipediaTheme(
            colors = darkColors
        ) else
            WikipediaTheme(
                colors = lightColors
            )
    }

    CompositionLocalProvider(
        LocalWikipediaColor provides wikipediaColorSystem
    ) {
        content()
    }
}
