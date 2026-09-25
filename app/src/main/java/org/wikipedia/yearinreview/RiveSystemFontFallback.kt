package org.wikipedia.yearinreview

import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import app.rive.runtime.kotlin.fonts.FontBytes
import app.rive.runtime.kotlin.fonts.FontFallbackStrategy
import app.rive.runtime.kotlin.fonts.FontHelper
import app.rive.runtime.kotlin.fonts.Fonts
import java.util.Locale
import kotlin.math.abs

private class RiveSystemFontFallback(private val langTags: List<String>) : FontFallbackStrategy {
    // Called by Rive's native code only when a character is missing, once per weight
    override fun getFont(weight: Fonts.Weight): List<FontBytes> {
        val families = FontHelper.getSystemFontList()
        return langTags.mapNotNull { tag ->
            val family = families.firstOrNull { family ->
                family.lang?.split(',', ' ')?.any { it.equals(tag, ignoreCase = true) } == true
            } ?: return@mapNotNull null
            // Entries with fallbackFor (e.g. "serif") are alternates of the family's main sans fonts
            family.fonts.values.flatten()
                .filter { it.style == Fonts.Font.STYLE_NORMAL && it.fallbackFor == null }
                .minByOrNull { abs(it.weight.weight - weight.weight) }
        }
            // CJK languages share one large .ttc file, so read each file only once
            .distinctBy { it.name }
            .mapNotNull { FontHelper.getFontBytes(it) }
    }
}

// Maps the UI locale to the lang tags used in the system fonts.xml
private fun fallbackLangTags(locale: Locale): List<String> {
    val localeTag = when (val script = ULocale.addLikelySubtags(ULocale.forLocale(locale)).script) {
        "Jpan" -> "ja"
        "Kore" -> "ko"
        "Hans" -> "zh-Hans"
        "Hant" -> "zh-Hant"
        "" -> null
        else -> "und-$script"
    }
    return (listOfNotNull(localeTag) + LIGHTWEIGHT_FALLBACK_LANGS).distinct()
}

@Composable
fun InstallRiveSystemFontFallback() {
    val locale = LocalConfiguration.current.locales[0]
    val fallback = remember(locale) { RiveSystemFontFallback(fallbackLangTags(locale)) }
    DisposableEffect(fallback) {
        val previousFallback = FontFallbackStrategy.stylePicker
        FontFallbackStrategy.stylePicker = fallback
        onDispose {
            if (FontFallbackStrategy.stylePicker === fallback) {
                FontFallbackStrategy.stylePicker = previousFallback
            }
        }
    }
}

// Small script fonts (~100-300 KB each) that are always offered, for text mixed with other scripts
private val LIGHTWEIGHT_FALLBACK_LANGS = listOf("und-Arab", "und-Hebr", "und-Deva")
