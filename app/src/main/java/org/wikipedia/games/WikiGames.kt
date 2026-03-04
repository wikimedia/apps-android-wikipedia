package org.wikipedia.games

import org.wikipedia.R
import org.wikipedia.language.LanguageUtil

enum class WikiGames(
    val titleRes: Int,
    val supportLanguages: List<String> = emptyList()
) {
    WHICH_CAME_FIRST(
        R.string.on_this_day_game_title,
        LanguageUtil.getSupportedLanguageCodes(listOf("en", "de", "fr", "es", "pt", "ru", "ar", "tr", "zh"))
    );

    fun isLangSupported(lang: String): Boolean {
        return supportLanguages.contains(lang)
    }

    fun isLangCodesSupported(langCodes: List<String>): Boolean {
        return langCodes.any { supportLanguages.contains(it) }
    }
}

enum class PlayTypes {
    PLAYED_ON_SAME_DAY, PLAYED_ON_ARCHIVE
}
