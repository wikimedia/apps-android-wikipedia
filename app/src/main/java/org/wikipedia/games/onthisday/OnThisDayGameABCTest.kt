package org.wikipedia.games.onthisday

import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.analytics.eventplatform.DailyStatsEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.random.Random

class OnThisDayGameABCTest : ABTest("onThisDayGame", GROUP_SIZE_3) {
    fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "game"
            GROUP_3 -> "newInstall"
            else -> "control"
        }
    }

    override fun assignGroup() {
        val installAgeMillis = DailyStatsEvent.getInstallTime(WikipediaApp.instance)
        val installDate = LocalDate.ofInstant(Instant.ofEpochMilli(installAgeMillis), ZoneOffset.UTC).plusDays(10)

        val gameDate = OnThisDayGameViewModel.dateReleasedForLang(WikipediaApp.instance.appOrSystemLanguageCode)
        val isAbTested = OnThisDayGameViewModel.isLangABTested(WikipediaApp.instance.appOrSystemLanguageCode)

        testGroup = if (installDate.isBefore(gameDate) && isAbTested) {
            Random(System.currentTimeMillis()).nextInt(Int.MAX_VALUE).mod(2)
        } else {
            GROUP_3
        }
    }
}
