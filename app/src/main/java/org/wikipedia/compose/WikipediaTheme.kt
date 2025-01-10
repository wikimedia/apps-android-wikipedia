package org.wikipedia.compose

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
        WikipediaThemeType.LIGHT -> LightColors
        WikipediaThemeType.DARK -> DarkColors
        WikipediaThemeType.BLACK -> BlackColors
        WikipediaThemeType.SEPIA -> SepiaColors
        WikipediaThemeType.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
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
