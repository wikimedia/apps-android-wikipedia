package org.wikipedia.games

import org.wikipedia.R
import org.wikipedia.language.LanguageUtil

enum class WikiGames(
    val titleRes: Int,
    private val baseLanguages: List<String> = emptyList()
) {
    WHICH_CAME_FIRST(
        titleRes = R.string.on_this_day_game_title,
        baseLanguages = listOf("en", "de", "fr", "es", "pt", "ru", "ar", "tr", "zh")
    );

    val supportLanguages: List<String> by lazy {
        LanguageUtil.getSupportedLanguageCodes(baseLanguages)
    }

    fun isLangSupported(vararg langCodes: String): Boolean {
        return langCodes.any { supportLanguages.contains(it) }
    }
}

enum class PlayTypes {
    PLAYED_ON_SAME_DAY, PLAYED_ON_ARCHIVE
}
