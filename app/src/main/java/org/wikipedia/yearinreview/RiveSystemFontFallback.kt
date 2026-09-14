package org.wikipedia.yearinreview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.rive.runtime.kotlin.core.Rive
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
fun rememberRiveSystemFontFallback() {
    val context = LocalContext.current
    val fallback = remember {
        Rive.init(context.applicationContext)
        RiveSystemFontFallback()
    }
    DisposableEffect(fallback) {
        FontFallbackStrategy.stylePicker = fallback
        onDispose {
            if (FontFallbackStrategy.stylePicker === fallback) {
                FontFallbackStrategy.stylePicker = null
            }
        }
    }
}
private val FALLBACK_FONT_LANGS = listOf("und-Arab", "ja", "und-Deva")
