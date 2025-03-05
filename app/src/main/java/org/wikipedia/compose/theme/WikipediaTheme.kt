package org.wikipedia.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.wikipedia.WikipediaApp
import org.wikipedia.theme.Theme

@Composable
fun BaseTheme(
    currentTheme: Theme = WikipediaApp.instance.currentTheme,
    content: @Composable () -> Unit
) {
    val appTheme by remember { mutableStateOf(currentTheme) }
    val wikipediaColorSystem = when (appTheme) {
        Theme.LIGHT -> LightColors
        Theme.DARK -> DarkColors
        Theme.BLACK -> BlackColors
        Theme.SEPIA -> SepiaColors
    }

    CompositionLocalProvider(
        LocalWikipediaColor provides wikipediaColorSystem,
        LocalWikipediaTypography provides Typography
    ) {
        content()
    }
}

object WikipediaTheme {
    val colors: WikipediaColor
        @Composable
        get() = LocalWikipediaColor.current

    val typography: WikipediaTypography
        @Composable
        get() = LocalWikipediaTypography.current
}
