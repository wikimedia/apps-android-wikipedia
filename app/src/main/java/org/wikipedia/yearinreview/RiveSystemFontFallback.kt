package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import app.rive.runtime.kotlin.fonts.FontBytes
import app.rive.runtime.kotlin.fonts.FontFallbackStrategy
import app.rive.runtime.kotlin.fonts.FontHelper
import app.rive.runtime.kotlin.fonts.Fonts

private class RiveSystemFontFallback : FontFallbackStrategy {
    // Called by Rive's native code only when a character is missing
    override fun getFont(weight: Fonts.Weight): List<FontBytes> =
        FALLBACK_FONT_LANGS.mapNotNull { lang ->
            val font = FontHelper.getFallbackFonts(Fonts.FontOpts(lang = lang, weight = weight))
                .ifEmpty { FontHelper.getFallbackFonts(Fonts.FontOpts(lang = lang, weight = null)) }
                .firstOrNull()
            font?.let { FontHelper.getFontBytes(it) }
        }
}

@Composable
fun InstallRiveSystemFontFallback() {
    val fallback = remember { RiveSystemFontFallback() }
    DisposableEffect(fallback) {
        FontFallbackStrategy.stylePicker = fallback
        onDispose {
            if (FontFallbackStrategy.stylePicker === fallback) {
                FontFallbackStrategy.stylePicker = null
            }
        }
    }
}
private val FALLBACK_FONT_LANGS = listOf("und-Arab", "ja", "zh-hans", "und-Deva")
