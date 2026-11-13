package org.wikipedia.yearinreview.data

import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.games.WikiGames

class YearInReviewEndScreenABTest : ABTest("year-in-review-end-screen", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
            else -> "control"
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    fun isUserEligible(): Boolean {
        return WikiGames.WHICH_CAME_FIRST.isLangSupported(WikipediaApp.instance.appOrSystemLanguageCode)
    }
}
