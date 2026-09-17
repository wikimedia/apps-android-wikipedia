package org.wikipedia.feed.readaloud

import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig

class ReadAloudLeadSectionABTest : ABTest("readAloudLeadSection", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "test"
            else -> "control"
        }
    }

    fun isTestGroupUser(): Boolean {
        return group == GROUP_2
    }

    fun isTestActive(): Boolean {
        return if (Prefs.readAloudLeadSectionOverrideConfig) true else (RemoteConfig.config.androidv1?.readAloudLeadSectionEnabled ?: false)
    }

    suspend fun shouldShowToolTip(): Boolean {
        return isTestActive() &&
                isTestGroupUser() &&
                ReadAloudArticlesRepository.isSupported(WikipediaApp.instance.wikiSite) &&
                !Prefs.readAloudLeadSectionTooltipShown &&
                Prefs.exploreFeedVisitCount > 0 &&
                AppDatabase.instance.topicInterestDao().hasAnyTopics()
    }
}
