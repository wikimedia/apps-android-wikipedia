package org.wikipedia.compose.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import org.wikipedia.WikipediaApp
import org.wikipedia.theme.Theme

@OptIn(ExperimentalMaterial3Api::class)
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

    val rippleConfig = RippleConfiguration(color = wikipediaColorSystem.overlayColor)

    CompositionLocalProvider(
        LocalWikipediaColor provides wikipediaColorSystem,
        LocalRippleConfiguration provides rippleConfig,
        LocalIndication provides ripple()
    ) {
        content()
    }
}

object WikipediaTheme {
    val colors: WikipediaColor
        @Composable
        get() = LocalWikipediaColor.current
}
