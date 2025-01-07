package org.wikipedia.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class WikipediaThemeType {
    SYSTEM, LIGHT, DARK, BLACK, SEPIA
}

// @TODO: add typography, shapes, dimensions
data class WikipediaThemeInfo(
    val colors: WikipediaColor
)

@Composable
fun MainTheme(
    wikipediaThemeType: WikipediaThemeType = WikipediaThemeType.SYSTEM,
    content: @Composable () -> Unit
) {
    val wikipediaColorSystem = when (wikipediaThemeType) {
        WikipediaThemeType.LIGHT -> WikipediaThemeInfo(
            colors = lightColors
        )

        WikipediaThemeType.DARK -> WikipediaThemeInfo(
            colors = darkColors
        )

        WikipediaThemeType.BLACK -> WikipediaThemeInfo(
            colors = blackColors
        )

        WikipediaThemeType.SEPIA -> WikipediaThemeInfo(
            colors = sepiaColors
        )

        WikipediaThemeType.SYSTEM -> if (isSystemInDarkTheme()) WikipediaThemeInfo(
            colors = darkColors
        ) else
            WikipediaThemeInfo(
                colors = lightColors
            )
    }

    CompositionLocalProvider(
        LocalWikipediaColor provides wikipediaColorSystem
    ) {
        content()
    }
}

object WikipediaTheme {
    val theme: WikipediaThemeInfo
    @Composable
    get() = LocalWikipediaColor.current
}
