package org.wikipedia.feed.readaloud

import kotlinx.coroutines.flow.first
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTest
import org.wikipedia.database.AppDatabase
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.settings.SettingsRepository
import org.wikipedia.settings.homefeed.ForYouModuleType

class ReadAloudLeadSectionABTest : ABTest("readaloudleadsection", GROUP_SIZE_2) {
    override fun getGroupName(): String {
        return when (group) {
            GROUP_2 -> "treatment"
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
                AppDatabase.instance.topicInterestDao().hasAnyTopics() &&
                !isModuleHidden()
    }

    private suspend fun isModuleHidden(): Boolean {
        return SettingsRepository.hiddenModules.first().contains(ForYouModuleType.READ_ALOUD_LEAD_SECTION.name)
    }
}
