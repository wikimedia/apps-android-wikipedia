package org.wikipedia.games

import org.wikipedia.R

enum class WikiGames(val titleRes: Int) {
    WHICH_CAME_FIRST(R.string.on_this_day_game_title)
}

enum class PlayTypes {
    PLAYED_ON_SAME_DAY, PLAYED_ON_ARCHIVE
}
